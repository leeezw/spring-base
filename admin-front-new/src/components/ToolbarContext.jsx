import { createContext, useContext, useState } from 'react';

const ToolbarContext = createContext(null);

export function ToolbarProvider({ children }) {
  const [toolbarContent, setToolbarContent] = useState(null);

  return (
    <ToolbarContext.Provider value={{ toolbarContent, setToolbarContent }}>
      {children}
    </ToolbarContext.Provider>
  );
}

export function useToolbar() {
  const context = useContext(ToolbarContext);
  if (!context) {
    throw new Error('useToolbar must be used within ToolbarProvider');
  }
  return context;
}

