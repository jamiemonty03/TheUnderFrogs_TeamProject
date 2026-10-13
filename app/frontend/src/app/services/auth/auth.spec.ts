import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Auth, CurrentUser, DEV_LOGIN, SESSION_KEY } from './auth';
import { authInterceptor } from './auth.interceptor';
import { API_BASE_URL } from '../api-config';
import { DevLogin } from '../../../environments/environment.model';
import { environment as developmentEnvironment } from '../../../environments/environment.development';

function jwt(expiresInSeconds: number): string {
  const encode = (value: object) => btoa(JSON.stringify(value)).replace(/=+$/, '');
  return `${encode({ alg: 'RS256' })}.${encode({ sub: '11', exp: Math.floor(Date.now() / 1000) + expiresInSeconds })}.signature`;
}

const demo: CurrentUser = { userId: 11, username: 'demo', roles: ['TRADER'], accountId: 'ACC0011' };

describe('Auth', () => {
  let auth: Auth;
  let http: HttpTestingController;

  function setUp(devLogin: DevLogin | null = null, apiBaseUrl = '') {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: DEV_LOGIN, useValue: devLogin },
        { provide: API_BASE_URL, useValue: apiBaseUrl },
      ],
    });
    auth = TestBed.inject(Auth);
    http = TestBed.inject(HttpTestingController);
  }

  async function logInAsDemo(accessToken = jwt(900)) {
    const result = firstValueFrom(auth.login('demo', 'Demo123!'));
    http
      .expectOne('/auth/login')
      .flush({ accessToken, refreshToken: 'refresh-1', expiresIn: 900, mfaRequired: false });
    http.expectOne('/auth/me').flush(demo);
    return result;
  }

  beforeEach(() => sessionStorage.clear());

  afterEach(() => http.verify());

  it('starts logged out', () => {
    setUp();

    expect(auth.isAuthenticated()).toBe(false);
    expect(auth.getToken()).toBeNull();
    expect(auth.currentUser()).toBeNull();
  });

  it('login() posts the credentials, then loads the current user with the new token', async () => {
    setUp();
    const accessToken = jwt(900);

    const result = firstValueFrom(auth.login('demo', 'Demo123!'));
    const login = http.expectOne('/auth/login');
    expect(login.request.body).toEqual({ username: 'demo', password: 'Demo123!' });
    expect(login.request.headers.has('Authorization')).toBe(false);
    login.flush({ accessToken, refreshToken: 'refresh-1', expiresIn: 900, mfaRequired: false });
    const me = http.expectOne('/auth/me');
    expect(me.request.headers.get('Authorization')).toBe(`Bearer ${accessToken}`);
    me.flush(demo);

    expect(await result).toEqual(demo);
    expect(auth.isAuthenticated()).toBe(true);
    expect(auth.getToken()).toBe(accessToken);
    expect(auth.currentUser()).toEqual(demo);
  });

  it('calls auth-service on the configured API origin, sending the token there for /auth/me', async () => {
    setUp(null, 'https://api.example.com');
    const accessToken = jwt(900);

    const result = firstValueFrom(auth.login('demo', 'Demo123!'));
    http
      .expectOne('https://api.example.com/auth/login')
      .flush({ accessToken, refreshToken: 'refresh-1', expiresIn: 900, mfaRequired: false });
    const me = http.expectOne('https://api.example.com/auth/me');
    expect(me.request.headers.get('Authorization')).toBe(`Bearer ${accessToken}`);
    me.flush(demo);

    expect(await result).toEqual(demo);
  });

  it('keeps the session across a page reload in the same tab', async () => {
    setUp();
    await logInAsDemo();
    const token = auth.getToken();

    TestBed.resetTestingModule();
    setUp();

    expect(auth.getToken()).toBe(token);
    expect(auth.currentUser()).toEqual(demo);
  });

  it('treats an expired token as logged out', async () => {
    setUp();
    await logInAsDemo(jwt(-60));

    expect(auth.isAuthenticated()).toBe(false);
    expect(auth.getToken()).toBeNull();
  });

  it('logout() revokes the refresh token on the server and clears the session', async () => {
    setUp();
    const accessToken = jwt(900);
    await logInAsDemo(accessToken);

    const result = firstValueFrom(auth.logout());
    const logout = http.expectOne('/auth/logout');
    expect(logout.request.body).toEqual({ refresh_token: 'refresh-1' });
    expect(logout.request.headers.get('Authorization')).toBe(`Bearer ${accessToken}`);
    logout.flush(null, { status: 204, statusText: 'No Content' });
    await result;

    expect(auth.isAuthenticated()).toBe(false);
    expect(auth.currentUser()).toBeNull();
    expect(sessionStorage.getItem(SESSION_KEY)).toBeNull();
  });

  it('logout() still clears the session when the server cannot be reached', async () => {
    setUp();
    await logInAsDemo();

    const result = firstValueFrom(auth.logout());
    http.expectOne('/auth/logout').error(new ProgressEvent('error'));
    await result;

    expect(auth.isAuthenticated()).toBe(false);
  });

  it('login() leaves the user logged out when the credentials are wrong', async () => {
    setUp();

    const result = firstValueFrom(auth.login('demo', 'wrong'));
    http
      .expectOne('/auth/login')
      .flush(
        { errorCode: 'AUTH-401', message: 'Invalid credentials' },
        { status: 401, statusText: 'Unauthorized' },
      );

    await expect(result).rejects.toMatchObject({ status: 401 });
    expect(auth.isAuthenticated()).toBe(false);
  });

  it('login() does not leave a half-logged-in session when loading the user fails', async () => {
    setUp();

    const result = firstValueFrom(auth.login('demo', 'Demo123!'));
    http.expectOne('/auth/login').flush({
      accessToken: jwt(900),
      refreshToken: 'refresh-1',
      expiresIn: 900,
      mfaRequired: false,
    });
    http.expectOne('/auth/me').flush(null, { status: 500, statusText: 'Server Error' });

    await expect(result).rejects.toMatchObject({ status: 500 });
    expect(auth.isAuthenticated()).toBe(false);
    expect(sessionStorage.getItem(SESSION_KEY)).toBeNull();
  });

  describe('development-only demo login', () => {
    it('the development environment holds the seeded demo user', () => {
      expect(developmentEnvironment.production).toBe(false);
      expect(developmentEnvironment.devLogin).toEqual({ username: 'demo', password: 'Demo123!' });
    });

    it('loginAsDevUser() logs in with the configured demo credentials', async () => {
      setUp({ username: 'demo', password: 'Demo123!' });

      const result = firstValueFrom(auth.loginAsDevUser());
      const login = http.expectOne('/auth/login');
      expect(login.request.body).toEqual({ username: 'demo', password: 'Demo123!' });
      login.flush({
        accessToken: jwt(900),
        refreshToken: 'refresh-1',
        expiresIn: 900,
        mfaRequired: false,
      });
      http.expectOne('/auth/me').flush(demo);

      expect(await result).toEqual(demo);
      expect(auth.isAuthenticated()).toBe(true);
    });

    it('loginAsDevUser() does nothing when no demo credentials are configured, as in production', async () => {
      setUp(null);

      expect(await firstValueFrom(auth.loginAsDevUser())).toBeNull();
      http.expectNone('/auth/login');
    });

    it('loginAsDevUser() does not log in again when a session already exists', async () => {
      setUp({ username: 'demo', password: 'Demo123!' });
      await logInAsDemo();

      expect(await firstValueFrom(auth.loginAsDevUser())).toEqual(demo);
      http.expectNone('/auth/login');
    });

    it('loginAsDevUser() lets the app start even if the auth service is down', async () => {
      setUp({ username: 'demo', password: 'Demo123!' });

      const result = firstValueFrom(auth.loginAsDevUser());
      http.expectOne('/auth/login').error(new ProgressEvent('error'));

      expect(await result).toBeNull();
    });
  });
});
