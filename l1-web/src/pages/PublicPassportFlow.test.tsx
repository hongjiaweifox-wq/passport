import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { PublicPassportSummaryPage } from './PublicPassportSummaryPage';
import { PublicAuthPage } from './PublicAuthPage';
import { getPublicPassport } from '../api/publicPassport';
vi.mock('../api/publicPassport', () => ({ getPublicPassport: vi.fn() }));
afterEach(() => { cleanup(); vi.clearAllMocks(); window.localStorage.clear(); window.location.hash = ''; });

describe('PublicPassportSummaryPage', () => {
  it('renders the passport header, tabs and general fields', async () => {
    vi.mocked(getPublicPassport).mockResolvedValue({
      schemaVersion: 'dpp-schema/1.0', passportStatus: 'PUBLISHED', passportVersion: 1, updatedAt: '2026-01-01',
      data: { model_code: 'M1', manufacturer_name: 'Maker', battery_category: 'EV', chemistry: 'LFP' },
    });
    render(<PublicPassportSummaryPage publicCode="c1"/>);
    await waitFor(() => expect(screen.getAllByText('M1').length).toBeGreaterThan(0));
    expect(screen.getByRole('tab', { name: 'General' })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: 'General' })).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByText('Manufactured by')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('tab', { name: 'Material composition' }));
    expect(await screen.findByText('LFP')).toBeInTheDocument();
  });
  it('shows carbon footprint stages and switches language', async () => {
    vi.mocked(getPublicPassport).mockResolvedValue({
      schemaVersion: 'dpp-schema/1.0', passportStatus: 'PUBLISHED', passportVersion: 1, updatedAt: '2026-01-01',
      data: {
        model_code: 'M1',
        contribution_of_raw_material_acquisition_and_pre_processing_lifecycle_stage: 89,
      },
    });
    render(<PublicPassportSummaryPage publicCode="c1"/>);
    await waitFor(() => expect(screen.getByRole('tab', { name: 'General' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('tab', { name: 'Carbon Footprint' }));
    expect(await screen.findByText('89 gCO₂e/kWh')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '中文' }));
    expect((await screen.findAllByText('基本信息')).length).toBeGreaterThan(0);
  });
});

describe('PublicAuthPage', () => {
  it('authenticates and returns to the callback path', () => {
    render(<PublicAuthPage callback="/p/c1/summary"/>);
    expect(screen.getByText('Authenticate with any of the below options')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /With verifiable credentials/ }));
    expect(window.localStorage.getItem('passport-public-authenticated')).toBe('1');
    expect(window.location.hash).toBe('#/p/c1/summary');
  });
});
