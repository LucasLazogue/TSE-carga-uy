import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { TooltipProvider } from '@/components/ui/tooltip'
import SessionProvider from '@/auth/SessionProvider'
import App from './App'
import './index.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <SessionProvider>
        <TooltipProvider>
          <App />
        </TooltipProvider>
      </SessionProvider>
    </BrowserRouter>
  </StrictMode>,
)
