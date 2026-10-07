import { Controller, Get, Header, Inject } from '@nestjs/common';
import { Public } from '../../common/decorators/public.decorator';
import type { SigningKey } from '../../config/signing-key';
import { SIGNING_KEY } from './token.service';

export interface Jwk {
  kty: 'RSA';
  use: 'sig';
  alg: 'RS256';
  kid: string;
  n: string;
  e: string;
}

export interface Jwks {
  keys: Jwk[];
}

@Controller('.well-known')
export class JwksController {
  private readonly jwks: Jwks;

  constructor(@Inject(SIGNING_KEY) signingKey: SigningKey) {
    const { n, e } = signingKey.publicKey.export({ format: 'jwk' });
    this.jwks = { keys: [{ kty: 'RSA', use: 'sig', alg: 'RS256', kid: signingKey.kid, n: n!, e: e! }] };
  }

  @Public()
  @Get('jwks.json')
  @Header('Cache-Control', 'public, max-age=300')
  getJwks(): Jwks {
    return this.jwks;
  }
}
