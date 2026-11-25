import { createBrowserRouter, Navigate } from 'react-router';
import { EscuchaSesionExpirada } from '../auth/EscuchaSesionExpirada';
import { LoginPage } from '../auth/LoginPage';
import { RequireAuth } from '../auth/RequireAuth';
import { PaginaNoEncontrada } from '../components/PaginaNoEncontrada';
import { ConductoresPage } from '../features/conductores/ConductoresPage';
import { DemoPage } from '../features/demo/DemoPage';
import { MonitoreoPage } from '../features/monitoreo/MonitoreoPage';
import { ProgramacionPage } from '../features/programacion/ProgramacionPage';
import { EditorRutaPage } from '../features/rutas/editor/EditorRutaPage';
import { RutasPage } from '../features/rutas/RutasPage';
import { VehiculosPage } from '../features/vehiculos/VehiculosPage';
import { AppLayout } from '../layout/AppLayout';

export const router = createBrowserRouter([
  {
    element: <EscuchaSesionExpirada />,
    children: [
      { path: '/login', element: <LoginPage /> },
      {
        element: <RequireAuth />,
        children: [
          {
            element: <AppLayout />,
            children: [
              { index: true, element: <Navigate to="/rutas" replace /> },
              { path: '/conductores', element: <ConductoresPage /> },
              { path: '/vehiculos', element: <VehiculosPage /> },
              { path: '/rutas', element: <RutasPage /> },
              { path: '/rutas/nueva', element: <EditorRutaPage /> },
              { path: '/rutas/:id', element: <EditorRutaPage /> },
              { path: '/programacion', element: <ProgramacionPage /> },
              { path: '/monitoreo', element: <MonitoreoPage /> },
              // Redirige a / si el backend no tiene el perfil demo.
              { path: '/demo', element: <DemoPage /> },
            ],
          },
        ],
      },
      { path: '*', element: <PaginaNoEncontrada /> },
    ],
  },
]);
