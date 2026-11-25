import { createBrowserRouter, Navigate } from 'react-router';
import { EscuchaSesionExpirada } from '../auth/EscuchaSesionExpirada';
import { LoginPage } from '../auth/LoginPage';
import { RequireAuth } from '../auth/RequireAuth';
import { PaginaNoEncontrada } from '../components/PaginaNoEncontrada';
import { PaginaProvisional } from '../components/PaginaProvisional';
import { ConductoresPage } from '../features/conductores/ConductoresPage';
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
              { path: '/programacion', element: <PaginaProvisional titulo="Programación del día" /> },
              { path: '/monitoreo', element: <PaginaProvisional titulo="Monitoreo" /> },
            ],
          },
        ],
      },
      { path: '*', element: <PaginaNoEncontrada /> },
    ],
  },
]);
