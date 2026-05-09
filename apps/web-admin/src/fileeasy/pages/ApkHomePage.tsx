import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getFileEasyHomeSummary } from '../../api/fileeasy';
import {
  FILEEASY_APK_PAGE_STORAGE_KEYS,
  readFileEasyApkOnboardingDone,
  readFileEasyApkPassword,
  writeFileEasyApkOnboardingDone,
  writeFileEasyApkPassword,
} from '../constants/apkRuntime';
import {
  FileEasyApkDialog,
  FileEasyApkHomeScene,
} from '../components/apk';
import type { ApkDialog, ApkMode, HomeSummary, ServiceStage, ServiceStep } from '../types/apk';
import './apk-home-page.css';

const ApkHomePage: React.FC = () => {
  const navigate = useNavigate();
  const [apkMode, setApkMode] = useState<ApkMode>(() =>
    readFileEasyApkOnboardingDone(FILEEASY_APK_PAGE_STORAGE_KEYS)
      ? 'normal'
      : 'first-install',
  );
  const [apkDialog, setApkDialog] = useState<ApkDialog>(() =>
    readFileEasyApkOnboardingDone(FILEEASY_APK_PAGE_STORAGE_KEYS)
      ? 'none'
      : 'welcome',
  );
  const [apkPasswordDraft, setApkPasswordDraft] = useState(() =>
    readFileEasyApkPassword(FILEEASY_APK_PAGE_STORAGE_KEYS),
  );
  const [apkPasswordCurrentInput, setApkPasswordCurrentInput] = useState(() =>
    readFileEasyApkPassword(FILEEASY_APK_PAGE_STORAGE_KEYS),
  );
  const [apkPasswordNext, setApkPasswordNext] = useState('');
  const [apkPasswordConfirm, setApkPasswordConfirm] = useState('');
  const [serviceStage, setServiceStage] = useState<ServiceStage>('booting');
  const [homeSummary, setHomeSummary] = useState<HomeSummary | null>(null);
  const [isHomeSummaryLoading, setIsHomeSummaryLoading] = useState(false);

  const serviceSteps = useMemo<ServiceStep[]>(
    () => [
      {
        key: 'ready',
        title: '扫码',
        detail: '让访客先扫描首页二维码进入上传页',
      },
      {
        key: 'foreground',
        title: '输入密码',
        detail: '进入上传页后输入访问密码完成验证',
      },
      {
        key: 'booting',
        title: '选择文件',
        detail: '验证通过后开始选择文件并查看上传状态',
      },
    ],
    [],
  );

  useEffect(() => {
    let active = true;

    const loadHomeSummary = async () => {
      setIsHomeSummaryLoading(true);
      try {
        const summary = await getFileEasyHomeSummary();
        if (!active) return;
        setHomeSummary({
          activeUploads: summary.activeUploads,
          recentFiles: summary.recentFiles,
          uploadUrl: summary.uploadUrl,
        });
      } catch (error) {
        console.warn('Failed to load FileEasy home summary', error);
      } finally {
        if (active) {
          setIsHomeSummaryLoading(false);
        }
      }
    };

    void loadHomeSummary();
    const timer = window.setInterval(() => {
      void loadHomeSummary();
    }, 4000);

    return () => {
      active = false;
      window.clearInterval(timer);
    };
  }, []);

  const openApkDialog = (dialog: ApkDialog) => {
    if (dialog === 'password') {
      setApkPasswordCurrentInput(apkPasswordDraft);
      setApkPasswordNext('');
      setApkPasswordConfirm('');
    }
    setApkDialog(dialog);
  };

  const applyApkMode = (mode: ApkMode) => {
    setApkMode(mode);
    if (mode === 'first-install') {
      setApkDialog('welcome');
      setServiceStage('booting');
      return;
    }
    if (mode === 'network-missing') {
      setApkDialog('network');
      setServiceStage('foreground');
      return;
    }
    setApkDialog('none');
    setServiceStage('ready');
  };

  const cycleServiceStage = () => {
    setServiceStage((current) => {
      if (current === 'booting') return 'foreground';
      if (current === 'foreground') return 'ready';
      return 'booting';
    });
  };

  const markOnboardingDone = () => {
    setApkMode('normal');
    setApkDialog('none');
    setServiceStage('ready');
    writeFileEasyApkOnboardingDone(FILEEASY_APK_PAGE_STORAGE_KEYS, true);
  };

  const saveApkPassword = () => {
    if (apkPasswordCurrentInput !== apkPasswordDraft) {
      return;
    }
    const nextPassword = apkPasswordNext || apkPasswordDraft;
    setApkPasswordDraft(nextPassword);
    setApkPasswordCurrentInput(nextPassword);
    setApkPasswordNext('');
    setApkPasswordConfirm('');
    setApkDialog('none');
    setApkMode('normal');
    setServiceStage('ready');
    writeFileEasyApkPassword(FILEEASY_APK_PAGE_STORAGE_KEYS, nextPassword);
  };

  return (
    <div className="fileeasy-apk-page">
      <FileEasyApkHomeScene
        apkMode={apkMode}
        apkPassword={apkPasswordDraft}
        homeSummary={homeSummary}
        isHomeSummaryLoading={isHomeSummaryLoading}
        serviceStage={serviceStage}
        serviceSteps={serviceSteps}
        onCompleteOnboarding={markOnboardingDone}
        onCycleServiceStage={cycleServiceStage}
        onJumpToAdmin={() => navigate('/admin')}
        onJumpToUpload={() => navigate('/')}
        onOpenInfoDialog={() => openApkDialog(apkMode === 'network-missing' ? 'network' : 'welcome')}
        onOpenNetworkDialog={() => openApkDialog('network')}
        onOpenPasswordDialog={() => openApkDialog('password')}
        onSelectMode={applyApkMode}
        onSelectServiceStage={setServiceStage}
        onSetReady={() => setServiceStage('ready')}
      />

      {apkDialog !== 'none' ? (
        <FileEasyApkDialog
          confirmPassword={apkPasswordConfirm}
          currentPassword={apkPasswordCurrentInput}
          displayPassword={apkPasswordDraft}
          dialog={apkDialog}
          nextPassword={apkPasswordNext}
          onClose={() => setApkDialog('none')}
          onCompleteOnboarding={markOnboardingDone}
          onConfirmPasswordChange={saveApkPassword}
          onConfirmPasswordInputChange={setApkPasswordConfirm}
          onCurrentPasswordChange={setApkPasswordCurrentInput}
          onGoToPasswordDialog={() => setApkDialog('password')}
          onNextPasswordChange={setApkPasswordNext}
          onRecoverNetwork={() => applyApkMode('normal')}
        />
      ) : null}
    </div>
  );
};

export default ApkHomePage;
