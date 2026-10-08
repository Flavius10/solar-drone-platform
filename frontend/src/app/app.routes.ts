import { Routes } from '@angular/router';
import { Dashboard } from './pages/dashboard/dashboard';
import { NewInspection } from './pages/new-inspection/new-inspection';
import { Reports } from './pages/reports/reports';
import { Login } from './pages/login/login';
import { FarmMap } from './pages/farm-map/farm-map';
import { authGuard } from './guards/auth.guard';
import { AuditLog } from './pages/audit-log/audit-log';
import { ForgotPassword } from './pages/forgot-password/forgot-password';
import { ResetPassword } from './pages/reset-password/reset-password';
import { UserManagement } from './pages/user-management/user-management';
import { WorkOrders } from './pages/work-orders/work-orders';
import { Profile } from './pages/profile/profile';
import { PanelHistory } from './pages/panel-history/panel-history';
import { FarmManagement } from './pages/farm-management/farm-management';
import { VideoInspection } from './pages/video-inspection/video-inspection';

export const routes: Routes = [
    { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
    { path: 'login', component: Login },
    { path: 'forgot-password', component: ForgotPassword },
    { path: 'reset-password', component: ResetPassword },
    { path: 'dashboard', component: Dashboard, canActivate: [authGuard] },
    { path: 'farm-map', component: FarmMap, canActivate: [authGuard] },
    { path: 'new-inspection', component: NewInspection, canActivate: [authGuard] },
    { path: 'video-inspection', component: VideoInspection, canActivate: [authGuard] },
    { path: 'reports', component: Reports, canActivate: [authGuard] },
    { path: 'audit-log', component: AuditLog, canActivate: [authGuard] },
    { path: 'work-orders', component: WorkOrders, canActivate: [authGuard] },
    { path: 'user-management', component: UserManagement, canActivate: [authGuard] },
    { path: 'profile', component: Profile, canActivate: [authGuard] },
    { path: 'panel-history', component: PanelHistory, canActivate: [authGuard] },
    { path: 'farm-management', component: FarmManagement, canActivate: [authGuard] },
];