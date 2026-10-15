import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { Profile, UserProfile } from './profile';

const demo: UserProfile = { username: 'demo', email: 'demo@toadstock.io', fullName: 'Demo Trader' };

describe('Profile', () => {
  let profile: Profile;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    profile = TestBed.inject(Profile);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads your profile', async () => {
    const result = firstValueFrom(profile.getProfile());
    const request = http.expectOne('/auth/profile');
    request.flush(demo);

    expect(request.request.method).toBe('GET');
    expect(await result).toEqual(demo);
  });

  it('sends only the changed fields with PATCH', async () => {
    const result = firstValueFrom(profile.updateProfile({ fullName: 'Kermit' }));
    const request = http.expectOne('/auth/profile');
    request.flush({ ...demo, fullName: 'Kermit' });

    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ fullName: 'Kermit' });
    expect((await result).fullName).toBe('Kermit');
  });
});
