export interface AuthUser {
  id: string;
  schoolId: string | null;
  username: string;
  email: string | null;
  firstName: string | null;
  lastName: string | null;
  displayName: string | null;
  locale: string;
  roles: string[];
  permissions: string[];
  mustChangePassword?: boolean;
  questionsSet?: boolean;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
  user: AuthUser;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RefreshRequest {
  refreshToken: string;
}

export interface SecurityQuestion {
  id: string;
  code: string;
}

export interface LinkedChild {
  studentId: string;
  name: string;
  admissionNo: string;
  className: string | null;
  sectionName: string | null;
}

export interface AppUser {
  id: string;
  schoolId: string;
  username: string;
  email: string | null;
  phone: string | null;
  firstName: string;
  lastName: string | null;
  displayName: string;
  locale: string;
  status: string;
  mustChangePassword: boolean;
  questionsSet: boolean;
  roles: string[];
  createdAt: string;
  lastLoginAt: string | null;
  temporaryPassword?: string | null;
}

export interface CredentialAudit {
  id: string;
  actorUserId: string | null;
  targetUserId: string | null;
  action: string;
  detail: string | null;
  createdAt: string;
}
