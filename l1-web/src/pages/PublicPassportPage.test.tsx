import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { PublicPassportPage } from './PublicPassportPage';
import { LayerApiError } from '../api/client';
import { getPublicPassport } from '../api/publicPassport';
vi.mock('../api/publicPassport', () => ({ getPublicPassport: vi.fn() }));
afterEach(() => { cleanup(); vi.clearAllMocks(); window.localStorage.clear(); });
describe('PublicPassportPage', () => {
  it('renders the verified passport header and public attributes', async () => {
    vi.mocked(getPublicPassport).mockResolvedValue({ schemaVersion: 'dpp-schema/1.0', passportStatus: 'PUBLISHED', passportVersion: 1, updatedAt: '2026-01-01', data: { model_code: 'M1', manufacturer_name: 'Maker' } });
    render(<PublicPassportPage publicCode="public-1"/>);
    await waitFor(() => expect(screen.getByText('M1')).toBeInTheDocument());
    expect(screen.getByText('Passport ID')).toBeInTheDocument();
    expect(screen.getByText('Verified')).toBeInTheDocument();
    expect(screen.getAllByText('Maker').length).toBeGreaterThan(0);
    expect(screen.queryByText(/sn/i)).not.toBeInTheDocument();
  });
  it('renders carbon footprint and recycled content sections when provided', async () => {
    vi.mocked(getPublicPassport).mockResolvedValue({
      schemaVersion: 'dpp-schema/1.0', passportStatus: 'PUBLISHED', passportVersion: 1, updatedAt: '2026-01-01',
      data: {
        model_code: 'M1',
        contribution_of_raw_material_acquisition_and_pre_processing_lifecycle_stage: 89,
        contribution_of_main_product_production_lifecycle_stage: 30,
        nickel_recycled_content_share: 17,
      },
    });
    render(<PublicPassportPage publicCode="public-1"/>);
    await waitFor(() => expect(screen.getByText('Summary report')).toBeInTheDocument());
    expect(screen.getAllByText(/raw material extraction: 89/).length).toBeGreaterThan(0);
    expect(screen.getByText('NICKEL')).toBeInTheDocument();
    expect(screen.getAllByText(/Recycled content share: 17%/).length).toBeGreaterThan(0);
    expect(screen.getAllByText('No data provided').length).toBe(3);
  });
  it('shows unavailable state from HTTP 410, not from message text', async () => {
    vi.mocked(getPublicPassport).mockRejectedValue(new LayerApiError(410, { code: 'L5_PASSPORT_UNAVAILABLE', message: 'Passport is unavailable', traceId: '' }));
    render(<PublicPassportPage publicCode="paused"/>);
    await waitFor(() => expect(screen.getByText('This passport is suspended or unavailable')).toBeInTheDocument());
  });
  it('switches the public page language between English and Chinese', async () => {
    vi.mocked(getPublicPassport).mockResolvedValue({ schemaVersion: 'dpp-schema/1.0', passportStatus: 'PUBLISHED', passportVersion: 1, updatedAt: '2026-01-01', data: { model_code: 'M1' } });
    render(<PublicPassportPage publicCode="public-1"/>);
    await waitFor(() => expect(screen.getByText('Passport ID')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: '中文' }));
    expect(await screen.findByText('护照标识')).toBeInTheDocument();
    expect(screen.getByText('摘要报告')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'EN' })).toBeInTheDocument();
  });
});
