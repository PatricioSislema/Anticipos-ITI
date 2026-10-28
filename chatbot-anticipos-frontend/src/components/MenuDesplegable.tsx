import React, { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { ChevronDown, ChevronRight } from 'lucide-react';

interface SubItem {
    label: string;
    path: string;
}

export interface MenuItemProps {
    label: string;
    icon: React.ReactNode;
    path?: string;
    subItems?: SubItem[];
    roles: string[];
}

export const MenuDesplegable = ({ item, rolActual }: { item: MenuItemProps, rolActual: string }) => {
    const location = useLocation();
    const [isOpen, setIsOpen] = useState(false);

    if (!item.roles.includes(rolActual)) return null;

    // Si tiene subItems, es un menú desplegable
    if (item.subItems && item.subItems.length > 0) {
        return (
            <div>
                <div
                    onClick={() => setIsOpen(!isOpen)}
                    style={{
                        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                        padding: '14px 24px', cursor: 'pointer', color: '#f3f4f6', fontWeight: 500
                    }}
                >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                        {item.icon} {item.label}
                    </div>
                    {isOpen ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
                </div>

                {isOpen && (
                    <div style={{ backgroundColor: '#6b1d2f', display: 'flex', flexDirection: 'column' }}>
                        {item.subItems.map(sub => (
                            <Link
                                key={sub.path}
                                to={sub.path}
                                style={{
                                    padding: '10px 24px 10px 56px', color: location.pathname === sub.path ? '#8C1D40' : '#d1d5db',
                                    backgroundColor: location.pathname === sub.path ? '#f4f6f9' : 'transparent',
                                    textDecoration: 'none', fontSize: '13px', fontWeight: 500
                                }}
                            >
                                {sub.label}
                            </Link>
                        ))}
                    </div>
                )}
            </div>
        );
    }

    // Si no tiene subItems, es un enlace normal
    return (
        <Link
            to={item.path || '#'}
            style={{
                display: 'flex', alignItems: 'center', gap: '12px', padding: '14px 24px',
                color: location.pathname === item.path ? '#8C1D40' : '#f3f4f6',
                backgroundColor: location.pathname === item.path ? '#f4f6f9' : 'transparent',
                textDecoration: 'none', fontWeight: 500
            }}
        >
            {item.icon} {item.label}
        </Link>
    );
};