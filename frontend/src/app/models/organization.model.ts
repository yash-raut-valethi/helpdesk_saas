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

  // Membership role of the authenticated user in this organization.
  // Expected values: ADMIN, AGENT, CUSTOMER.
  role?: string;

  [key: string]: unknown;
}
