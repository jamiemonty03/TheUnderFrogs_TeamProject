import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { Auth, CurrentUser } from './auth';
import { Portfolio } from './interfaces/portfolios.interfaces';
import { Portfolios } from './portfolios';

const ACCOUNT = 'ACC0011';
const MAIN: Portfolio = {
  portfolioId: 'PF0011',
  accountId: ACCOUNT,
  name: 'Main',
  risk: 'MEDIUM',
  benchmark: null,
  defaultPortfolio: true,
};
const GROWTH: Portfolio = {
  portfolioId: 'PF0012',
  accountId: ACCOUNT,
  name: 'Growth',
  risk: 'HIGH',
  benchmark: 'Nasdaq-100',
  defaultPortfolio: false,
};

describe('Portfolios', () => {
  let service: Portfolios;
  let http: HttpTestingController;
  const currentUser = signal<CurrentUser | null>(null);

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Auth, useValue: { currentUser } },
      ],
    });
    service = TestBed.inject(Portfolios);
    http = TestBed.inject(HttpTestingController);
    currentUser.set({ userId: 11, username: 'demo', roles: ['TRADER'], accountId: ACCOUNT });
  });

  afterEach(() => http.verify());

  it("lists the signed-in account's portfolios", async () => {
    const result = firstValueFrom(service.getPortfolios());

    const req = http.expectOne(`/api/portfolios/${ACCOUNT}`);
    expect(req.request.method).toBe('GET');
    req.flush([MAIN, GROWTH]);

    expect((await result).map((p) => p.name)).toEqual(['Main', 'Growth']);
  });

  it('gets one portfolio', async () => {
    const result = firstValueFrom(service.getPortfolio('PF0012'));

    http.expectOne(`/api/portfolios/${ACCOUNT}/PF0012`).flush(GROWTH);

    expect(await result).toEqual(GROWTH);
  });

  it('creates a portfolio with a POST', async () => {
    const result = firstValueFrom(
      service.createPortfolio({ name: 'Growth', risk: 'HIGH', benchmark: 'Nasdaq-100' }),
    );

    const req = http.expectOne(`/api/portfolios/${ACCOUNT}`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ name: 'Growth', risk: 'HIGH', benchmark: 'Nasdaq-100' });
    req.flush(GROWTH, { status: 201, statusText: 'Created' });

    expect((await result).portfolioId).toBe('PF0012');
  });

  it('sends only the changed fields when updating', async () => {
    const result = firstValueFrom(service.updatePortfolio('PF0012', { risk: 'MEDIUM' }));

    const req = http.expectOne(`/api/portfolios/${ACCOUNT}/PF0012`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ risk: 'MEDIUM' });
    req.flush({ ...GROWTH, risk: 'MEDIUM' });

    expect((await result).risk).toBe('MEDIUM');
  });

  it('deletes a portfolio', async () => {
    const result = firstValueFrom(service.deletePortfolio('PF0012'), { defaultValue: undefined });

    const req = http.expectOne(`/api/portfolios/${ACCOUNT}/PF0012`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });

    await result;
    expect(service.error()).toBeNull();
  });

  it("shows the server's message, e.g. when deleting Main", async () => {
    const result = firstValueFrom(service.deletePortfolio('PF0011'));

    http
      .expectOne(`/api/portfolios/${ACCOUNT}/PF0011`)
      .flush(
        { errorCode: 'PFL-409', message: "The Main portfolio can't be deleted" },
        { status: 409, statusText: 'Conflict' },
      );

    await expect(result).rejects.toBeTruthy();
    expect(service.error()).toBe("The Main portfolio can't be deleted");
    expect(service.loading()).toBe(false);
  });

  it('falls back to a plain message when the server sends no error body', async () => {
    const result = firstValueFrom(service.getPortfolios());

    http
      .expectOne(`/api/portfolios/${ACCOUNT}`)
      .flush(null, { status: 503, statusText: 'Unavailable' });

    await expect(result).rejects.toBeTruthy();
    expect(service.error()).toBe("Couldn't update your portfolios. Try again in a moment.");
  });

  it('makes no request when the user has no trading account', async () => {
    currentUser.set({ userId: 1, username: 'admin', roles: ['ADMIN'], accountId: null });

    await expect(firstValueFrom(service.getPortfolios())).rejects.toBeTruthy();

    http.expectNone(() => true);
    expect(service.error()).toBe('No trading account is linked to this user.');
  });

  it('is loading only while a request is in flight', async () => {
    const result = firstValueFrom(service.getPortfolios());
    expect(service.loading()).toBe(true);

    http.expectOne(`/api/portfolios/${ACCOUNT}`).flush([MAIN]);
    await result;

    expect(service.loading()).toBe(false);
  });
});
