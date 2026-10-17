import React from 'react';
import type { DashboardStats } from '../types/database';
import { Library, CheckCircle2, FileEdit } from 'lucide-react';

interface StatsCardsProps {
  stats: DashboardStats;
  loading: boolean;
}

export const StatsCards: React.FC<StatsCardsProps> = ({ stats, loading }) => {
  return (
    <div className="stats-grid">
      <div className="stat-card">
        <div className="stat-icon-wrapper total-icon">
          <Library size={24} />
        </div>
        <div className="stat-content">
          <span className="stat-label">Total Books</span>
          <h3 className="stat-value">{loading ? '...' : stats.total}</h3>
          <span className="stat-subtext">Catalog documents</span>
        </div>
      </div>

      <div className="stat-card">
        <div className="stat-icon-wrapper published-icon">
          <CheckCircle2 size={24} />
        </div>
        <div className="stat-content">
          <span className="stat-label">Published</span>
          <h3 className="stat-value">{loading ? '...' : stats.published}</h3>
          <span className="stat-subtext">Visible to Android members</span>
        </div>
      </div>

      <div className="stat-card">
        <div className="stat-icon-wrapper draft-icon">
          <FileEdit size={24} />
        </div>
        <div className="stat-content">
          <span className="stat-label">Draft / Unpublished</span>
          <h3 className="stat-value">{loading ? '...' : stats.unpublished}</h3>
          <span className="stat-subtext">Internal review only</span>
        </div>
      </div>
    </div>
  );
};
