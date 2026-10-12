import { Inject, Injectable, UnauthorizedException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { JwtService } from '@nestjs/jwt';
import type { SigningKey } from '../../config/signing-key';

export const SIGNING_KEY = Symbol('SIGNING_KEY');
export const SERVICE_ROLE = 'SERVICE';
export const SERVICE_TOKEN_TTL_SECONDS = 60;

export interface TokenSubject {
  id: number;
  username: string;
  roles: string[];
  account_id: string | null;
}

export interface AccessTokenClaims {
  sub: string;
  username: string;
  roles: string[];
  accountId: string | null;
  iss: string;
  aud: string;
  iat: number;
  exp: number;
}

export interface IssuedAccessToken {
  accessToken: string;
  expiresIn: number;
}

export interface DecodedAccessToken {
  header: { alg: string; kid?: string; typ?: string };
  claims: AccessTokenClaims;
}

@Injectable()
export class TokenService {
  private readonly jwt = new JwtService();
  private readonly issuer: string;
  private readonly audience: string;
  private readonly ttlSeconds: number;
  private readonly publicKeyPem: string;

  constructor(
    @Inject(SIGNING_KEY) private readonly signingKey: SigningKey,
    configService: ConfigService,
  ) {
    this.issuer = configService.get<string>('JWT_ISSUER', 'auth-service');
    this.audience = configService.get<string>('JWT_AUDIENCE', 'trading-platform');
    this.ttlSeconds = configService.get<number>('JWT_ACCESS_TOKEN_TTL', 900);
    this.publicKeyPem = signingKey.publicKey.export({ type: 'spki', format: 'pem' }).toString();
  }

  issue(subject: TokenSubject): IssuedAccessToken {
    const accessToken = this.sign(
      { username: subject.username, roles: subject.roles, accountId: subject.account_id },
      String(subject.id),
      this.ttlSeconds,
    );
    return { accessToken, expiresIn: this.ttlSeconds };
  }

  issueServiceToken(serviceName: string): string {
    return this.sign({ roles: [SERVICE_ROLE] }, serviceName, SERVICE_TOKEN_TTL_SECONDS);
  }

  private sign(payload: object, subject: string, expiresIn: number): string {
    return this.jwt.sign(payload, {
      algorithm: 'RS256',
      privateKey: this.signingKey.privateKey,
      keyid: this.signingKey.kid,
      subject,
      issuer: this.issuer,
      audience: this.audience,
      expiresIn,
    });
  }

  verify(token: string): AccessTokenClaims {
    const decoded = this.decode(token);
    if (!decoded || decoded.header.kid !== this.signingKey.kid) {
      throw new UnauthorizedException('Invalid or expired token');
    }
    try {
      return this.jwt.verify<AccessTokenClaims>(token, {
        algorithms: ['RS256'],
        publicKey: this.publicKeyPem,
        issuer: this.issuer,
        audience: this.audience,
      });
    } catch {
      throw new UnauthorizedException('Invalid or expired token');
    }
  }

  decode(token: string): DecodedAccessToken | null {
    const decoded = this.jwt.decode(token, { complete: true }) as unknown as {
      header: DecodedAccessToken['header'];
      payload: AccessTokenClaims;
    } | null;
    if (!decoded || typeof decoded.payload !== 'object') {
      return null;
    }
    return { header: decoded.header, claims: decoded.payload };
  }
}
