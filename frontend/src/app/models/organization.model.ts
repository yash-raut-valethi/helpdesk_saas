export interface OrganizationRequest {
  name: string;
  description: string;
  slug: string;
  contactEmail: string;
  contactPhone: string;
  websiteUrl: string;
  logoUrl: string;
  address: string;
  creatorEmail: string;
}

export interface OrganizationResponse {
  id?: string | number;
  name?: string;
  description?: string;
  slug?: string;
  contactEmail?: string;
  contactPhone?: string;
  websiteUrl?: string;
  logoUrl?: string;
  address?: string;
  [key: string]: unknown;
}
