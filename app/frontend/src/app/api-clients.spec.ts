import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { firstValueFrom } from 'rxjs';
import { provideApiClients } from './api-clients';
import { AuthenticationService } from './api/auth/services/authentication.service';
import { AuthTokens } from './api/auth/models/auth-tokens';
import { environment } from '../environments/environment';

describe('Generated auth client', () => {
  const tokens: AuthTokens = {
    accessToken: 'access',
    refreshToken: 'refresh',
    expiresIn: 900,
    mfaRequired: false,
  };

  let httpController: HttpTestingController | undefined;

  function setUp(baseUrls = environment.apiBaseUrls) {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideApiClients(baseUrls)],
    });
    httpController = TestBed.inject(HttpTestingController);
    return { auth: TestBed.inject(AuthenticationService), http: httpController };
  }

  afterEach(() => {
    httpController?.verify();
    httpController = undefined;
  });

  it('calls /auth/login on the same origin by default, never a protocol-relative //auth URL', async () => {
    const { auth, http } = setUp();

    const result = firstValueFrom(auth.login({ body: { username: 'demo', password: 'Demo123!' } }));
    const request = http.expectOne('/auth/login');
    request.flush(tokens);

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ username: 'demo', password: 'Demo123!' });
    expect(await result).toEqual(tokens);
  });

  it('uses the configured base URL', async () => {
    const { auth, http } = setUp({ auth: 'https://auth.example.com' });

    const result = firstValueFrom(auth.refresh({ body: { refresh_token: 'refresh' } }));
    http.expectOne('https://auth.example.com/auth/refresh').flush(tokens);

    expect((await result).accessToken).toBe('access');
  });

  it('sends logout to /auth/logout and expects no content', async () => {
    const { auth, http } = setUp();

    const result = firstValueFrom(auth.logout({ body: { refresh_token: 'refresh' } }));
    const request = http.expectOne('/auth/logout');
    request.flush(null, { status: 204, statusText: 'No Content' });

    expect(request.request.method).toBe('POST');
    expect(await result).toBeNull();
  });

  it('defaults every environment to same-origin URLs served through the proxy', () => {
    expect(environment.apiBaseUrls.auth).toBe('');
  });
});
