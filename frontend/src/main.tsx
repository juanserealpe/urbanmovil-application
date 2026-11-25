import '@mantine/core/styles.css';
import '@mantine/notifications/styles.css';
import '@mantine/dates/styles.css';
import './global.css';

import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './app/App';

const raiz = document.getElementById('root');
if (!raiz) throw new Error('No se encontró el elemento #root');

createRoot(raiz).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
