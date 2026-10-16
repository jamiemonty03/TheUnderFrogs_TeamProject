export interface UserProfile {
  username: string;
  email: string;
  fullName: string | null;
}

export interface ProfileChanges {
  fullName?: string;
  email?: string;
}
