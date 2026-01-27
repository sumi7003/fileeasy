import React, { useEffect, useMemo, useState } from 'react';
import { Button, Drawer, Grid, Layout, Menu, Space, theme } from 'antd';
import {
  LaptopOutlined,
  MenuOutlined,
  UnorderedListOutlined,
  UserOutlined,
  VideoCameraOutlined,
  SwapOutlined,
} from '@ant-design/icons';
import { BrowserRouter as Router, Link, Route, Routes, useLocation } from 'react-router-dom';
import DeviceList from './pages/DeviceList';
import MediaLibrary from './pages/MediaLibrary';
import PlaylistManager from './pages/PlaylistManager';
import Settings from './pages/Settings';
import TransferStation from './pages/TransferStation';

const { Header, Content, Sider } = Layout;

const transferEnabled = false;

const menuItems = [
  {
    key: 'devices',
    icon: <LaptopOutlined />,
    label: <Link to="/">设备管理</Link>,
  },
  {
    key: 'media',
    icon: <VideoCameraOutlined />,
    label: <Link to="/media">素材库</Link>,
  },
  ...(transferEnabled
    ? [
        {
          key: 'transfer',
          icon: <SwapOutlined />,
          label: <Link to="/transfer">文件中转</Link>,
        },
      ]
    : []),
  {
    key: 'playlists',
    icon: <UnorderedListOutlined />,
    label: <Link to="/playlists">播放列表</Link>,
  },
  {
    key: 'settings',
    icon: <UserOutlined />,
    label: <Link to="/settings">系统设置</Link>,
  },
];

const AppLayout: React.FC = () => {
  const {
    token: { colorBgContainer },
  } = theme.useToken();
  const screens = Grid.useBreakpoint();
  const isMobile = !screens.md;
  const [menuOpen, setMenuOpen] = useState(false);
  const location = useLocation();

  const Logo = () => (
    <div style={{ 
      display: 'flex', 
      alignItems: 'center', 
      gap: '12px',
      padding: '4px 8px',
    }}>
      <div style={{
        width: '32px',
        height: '32px',
        background: 'linear-gradient(135deg, #1677ff 0%, #003eb3 100%)',
        borderRadius: '8px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        color: 'white',
        fontSize: '20px',
        fontWeight: 'bold',
        boxShadow: '0 2px 8px rgba(22, 119, 255, 0.3)',
        border: '1px solid rgba(255, 255, 255, 0.1)'
      }}>
        X
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', lineHeight: 1 }}>
        <span style={{ 
          color: 'white', 
          fontSize: '1.2rem', 
          fontWeight: 700,
          letterSpacing: '0.5px'
        }}>
          Xplay
        </span>
        <span style={{ 
          color: 'rgba(255, 255, 255, 0.65)', 
          fontSize: '0.7rem',
          fontWeight: 500,
          marginTop: '2px',
          textTransform: 'uppercase',
          letterSpacing: '1px'
        }}>
          Admin Center
        </span>
      </div>
    </div>
  );

  const selectedKey = useMemo(() => {
    if (location.pathname.startsWith('/media')) return 'media';
    if (transferEnabled && location.pathname.startsWith('/transfer')) return 'transfer';
    if (location.pathname.startsWith('/playlists')) return 'playlists';
    if (location.pathname.startsWith('/settings')) return 'settings';
    return 'devices';
  }, [location.pathname]);

  useEffect(() => {
    if (!isMobile) {
      setMenuOpen(false);
    }
  }, [isMobile]);

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ display: 'flex', alignItems: 'center', padding: '0 16px' }}>
        <Space size="middle" align="center">
          {isMobile && (
            <Button
              type="text"
              icon={<MenuOutlined style={{ color: 'white' }} />}
              onClick={() => setMenuOpen(true)}
            />
          )}
          <Logo />
        </Space>
      </Header>
      <Layout>
        {!isMobile && (
          <Sider width={200} style={{ background: colorBgContainer }}>
            <Menu
              mode="inline"
              selectedKeys={[selectedKey]}
              style={{ height: '100%', borderRight: 0 }}
              items={menuItems}
            />
          </Sider>
        )}
        <Layout style={{ padding: isMobile ? '12px' : '24px' }}>
          <Content
            style={{
              padding: isMobile ? 12 : 24,
              margin: 0,
              minHeight: 280,
              background: colorBgContainer,
              overflow: 'auto',
            }}
          >
            <Routes>
              <Route path="/" element={<DeviceList />} />
              <Route path="/media" element={<MediaLibrary />} />
              {transferEnabled && <Route path="/transfer" element={<TransferStation />} />}
              <Route path="/playlists" element={<PlaylistManager />} />
              <Route path="/settings" element={<Settings />} />
            </Routes>
          </Content>
        </Layout>
      </Layout>
      <Drawer
        placement="left"
        width={220}
        open={menuOpen}
        onClose={() => setMenuOpen(false)}
        bodyStyle={{ padding: 0 }}
      >
        <Menu
          mode="inline"
          selectedKeys={[selectedKey]}
          style={{ height: '100%', borderRight: 0 }}
          items={menuItems}
          onClick={() => setMenuOpen(false)}
        />
      </Drawer>
    </Layout>
  );
};

const App: React.FC = () => {
  return (
    <Router>
      <AppLayout />
    </Router>
  );
};

export default App;
