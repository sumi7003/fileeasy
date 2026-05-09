export type AuthSession = {
  token: string;
  expiresAt?: string;
};

export type LoginPayload = {
  password: string;
};

export type LoginResponse = {
  token?: string;
  expiresAt?: string;
};
