import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { vi } from 'vitest';
import { Auth, CurrentUser, SESSION_KEY } from './auth';
import { authInterceptor } from './auth.interceptor';
import { API_BASE_URL } from '../api-config';
import { environment as productionEnvironment } from '../../../environments/environment';
import { environment as developmentEnvironment } from '../../../environments/environment.development';

function jwt(expiresInSeconds: number): string {
  const encode = (value: object) => btoa(JSON.stringify(value)).replace(/=+$/, '');
  return `${encode({ alg: 'RS256' })}.${encode({ sub: '11', exp: Math.floor(Date.now() / 1000) + expiresInSeconds })}.signature`;
}

const demo: CurrentUser = { userId: 11, username: 'demo', roles: ['TRADER'], accountId: 'ACC0011' };

describe('Auth', () => {
  let auth: Auth;
  let http: HttpTestingController;

  function setUp(apiBaseUrl = '') {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
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
    setUp('https://api.example.com');
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

  it('has no demo login: no credentials in any environment and no automatic login', () => {
    setUp();

    expect(Object.keys(productionEnvironment).sort()).toEqual(['apiBaseUrl', 'production']);
    expect(Object.keys(developmentEnvironment).sort()).toEqual(['apiBaseUrl', 'production']);
    expect('loginAsDevUser' in auth).toBe(false);
    expect(auth.isAuthenticated()).toBe(false);
    http.expectNone('/auth/login');
  });

  describe('register()', () => {
    const details = {
      fullName: 'Kermit T. Frog',
      username: 'kermit',
      email: 'kermit@pond.io',
      password: 'correct-horse-battery',
    };
    const kermit: CurrentUser = {
      userId: 26,
      username: 'kermit',
      roles: ['TRADER'],
      accountId: 'ACC0012',
    };
    const created = (accessToken: string) => ({
      accessToken,
      refreshToken: 'refresh-new',
      expiresIn: 900,
      mfaRequired: false,
      accountId: 'ACC0012',
    });

    it('posts the details with full_name as the server expects, without a token', async () => {
      setUp();

      const result = firstValueFrom(auth.register(details));
      const request = http.expectOne('/auth/register');
      expect(request.request.method).toBe('POST');
      expect(request.request.body).toEqual({
        username: 'kermit',
        email: 'kermit@pond.io',
        password: 'correct-horse-battery',
        full_name: 'Kermit T. Frog',
      });
      expect(request.request.headers.has('Authorization')).toBe(false);
      request.flush(created(jwt(900)), { status: 201, statusText: 'Created' });
      http.expectOne('/auth/me').flush(kermit);

      expect(await result).toEqual(kermit);
    });

    it('signs the new user in: stores the session and loads currentUser, including the new account ID', async () => {
      setUp();
      const accessToken = jwt(900);

      const result = firstValueFrom(auth.register(details));
      http
        .expectOne('/auth/register')
        .flush(created(accessToken), { status: 201, statusText: 'Created' });
      const me = http.expectOne('/auth/me');
      expect(me.request.headers.get('Authorization')).toBe(`Bearer ${accessToken}`);
      me.flush(kermit);
      await result;

      expect(auth.isAuthenticated()).toBe(true);
      expect(auth.getToken()).toBe(accessToken);
      expect(auth.currentUser()?.accountId).toBe('ACC0012');
      expect(JSON.parse(sessionStorage.getItem(SESSION_KEY)!)).toEqual({
        accessToken,
        refreshToken: 'refresh-new',
        user: kermit,
      });
    });

    it('leaves the user signed out when the username or email is taken', async () => {
      setUp();

      const result = firstValueFrom(auth.register(details));
      http
        .expectOne('/auth/register')
        .flush(
          { errorCode: 'AUTH-409', message: 'User with username kermit already exists' },
          { status: 409, statusText: 'Conflict' },
        );

      await expect(result).rejects.toMatchObject({ status: 409 });
      expect(auth.isAuthenticated()).toBe(false);
      expect(sessionStorage.getItem(SESSION_KEY)).toBeNull();
    });

    it('does not leave a half-signed-in session when loading the user fails', async () => {
      setUp();

      const result = firstValueFrom(auth.register(details));
      http
        .expectOne('/auth/register')
        .flush(created(jwt(900)), { status: 201, statusText: 'Created' });
      http.expectOne('/auth/me').flush(null, { status: 500, statusText: 'Server Error' });

      await expect(result).rejects.toMatchObject({ status: 500 });
      expect(auth.isAuthenticated()).toBe(false);
      expect(sessionStorage.getItem(SESSION_KEY)).toBeNull();
    });

    it('calls auth-service on the configured API origin', async () => {
      setUp('https://api.example.com');

      const result = firstValueFrom(auth.register(details));
      http
        .expectOne('https://api.example.com/auth/register')
        .flush(created(jwt(900)), { status: 201, statusText: 'Created' });
      http.expectOne('https://api.example.com/auth/me').flush(kermit);

      expect(await result).toEqual(kermit);
    });
  });

  describe('token storage and logging rules', () => {
    const PASSWORD = 'Demo123!';
    const consoleMethods = ['log', 'info', 'warn', 'error', 'debug'] as const;
    let consoleSpies: ReturnType<typeof vi.spyOn>[];
    let localStorageWrites: string[];

    beforeEach(() => {
      localStorage.clear();
      consoleSpies = consoleMethods.map((method) =>
        vi.spyOn(console, method).mockImplementation(() => undefined),
      );
      localStorageWrites = [];
      const setItem = Storage.prototype.setItem;
      vi.spyOn(Storage.prototype, 'setItem').mockImplementation(function (
        this: Storage,
        key: string,
        value: string,
      ) {
        if (this === localStorage) {
          localStorageWrites.push(key);
        }
        return setItem.call(this, key, value);
      });
    });

    afterEach(() => vi.restoreAllMocks());

    const consoleOutput = () => JSON.stringify(consoleSpies.flatMap((spy) => spy.mock.calls));

    it('keeps the token in memory: it still works after sessionStorage is wiped', async () => {
      setUp();
      const accessToken = jwt(900);
      await logInAsDemo(accessToken);

      sessionStorage.clear();

      expect(auth.getToken()).toBe(accessToken);
      expect(auth.currentUser()).toEqual(demo);
    });

    it('backs the session up in sessionStorage only, never localStorage', async () => {
      setUp();
      const accessToken = jwt(900);
      await logInAsDemo(accessToken);

      const backup = JSON.parse(sessionStorage.getItem(SESSION_KEY)!);
      expect(backup).toEqual({ accessToken, refreshToken: 'refresh-1', user: demo });
      expect(localStorage.length).toBe(0);
      expect(localStorageWrites).toEqual([]);
    });

    it('never stores the password anywhere', async () => {
      setUp();
      await logInAsDemo();

      expect(sessionStorage.getItem(SESSION_KEY)).not.toContain(PASSWORD);
      expect(localStorage.length).toBe(0);
    });

    it('writes nothing to localStorage across a failed login, a login and a logout', async () => {
      setUp();
      const failed = firstValueFrom(auth.login('demo', 'wrong'));
      http.expectOne('/auth/login').flush(null, { status: 401, statusText: 'Unauthorized' });
      await expect(failed).rejects.toBeTruthy();
      await logInAsDemo();
      const loggedOut = firstValueFrom(auth.logout());
      http.expectOne('/auth/logout').flush(null, { status: 204, statusText: 'No Content' });
      await loggedOut;

      expect(localStorageWrites).toEqual([]);
      expect(localStorage.length).toBe(0);
    });

    it('registering writes nothing to localStorage, never stores the password and logs nothing', async () => {
      setUp();
      const accessToken = jwt(900);

      const result = firstValueFrom(
        auth.register({
          fullName: 'Kermit',
          username: 'kermit',
          email: 'kermit@pond.io',
          password: 'correct-horse-battery',
        }),
      );
      http
        .expectOne('/auth/register')
        .flush(
          {
            accessToken,
            refreshToken: 'refresh-new',
            expiresIn: 900,
            mfaRequired: false,
            accountId: 'ACC0012',
          },
          { status: 201, statusText: 'Created' },
        );
      http
        .expectOne('/auth/me')
        .flush({ userId: 26, username: 'kermit', roles: ['TRADER'], accountId: 'ACC0012' });
      await result;

      expect(localStorageWrites).toEqual([]);
      expect(sessionStorage.getItem(SESSION_KEY)).not.toContain('correct-horse-battery');
      expect(consoleOutput()).not.toContain(accessToken);
      expect(consoleOutput()).not.toContain('correct-horse-battery');
      consoleSpies.forEach((spy) => expect(spy).not.toHaveBeenCalled());
    });

    it('logs nothing at all, so never a token or password, across login, failures and logout', async () => {
      setUp();
      const accessToken = jwt(900);
      const failed = firstValueFrom(auth.login('demo', 'wrong-password'));
      http
        .expectOne('/auth/login')
        .flush(
          { errorCode: 'AUTH-401', message: 'Invalid credentials' },
          { status: 401, statusText: 'Unauthorized' },
        );
      await expect(failed).rejects.toBeTruthy();
      await logInAsDemo(accessToken);
      const loggedOut = firstValueFrom(auth.logout());
      http.expectOne('/auth/logout').error(new ProgressEvent('error'));
      await loggedOut;

      expect(consoleOutput()).not.toContain(accessToken);
      expect(consoleOutput()).not.toContain('refresh-1');
      expect(consoleOutput()).not.toContain(PASSWORD);
      expect(consoleOutput()).not.toContain('wrong-password');
      consoleSpies.forEach((spy) => expect(spy).not.toHaveBeenCalled());
    });
  });
});
