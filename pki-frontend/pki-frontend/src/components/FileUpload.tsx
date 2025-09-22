import React, { useState, useRef } from 'react';

interface FileUploadProps {
  accept?: string;
  multiple?: boolean;
  maxSize?: number; // in MB
  onFileSelect: (files: File[]) => void;
  disabled?: boolean;
  className?: string;
  children?: React.ReactNode;
}

const FileUpload: React.FC<FileUploadProps> = ({
  accept = '*/*',
  multiple = false,
  maxSize = 10,
  onFileSelect,
  disabled = false,
  children
}) => {
  const [dragActive, setDragActive] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const validateFile = (file: File): string | null => {
    if (maxSize && file.size > maxSize * 1024 * 1024) {
      return `Файл "${file.name}" превышает максимальный размер ${maxSize} МБ`;
    }
    return null;
  };

  const handleFiles = (files: FileList | null) => {
    if (!files) return;

    const fileArray = Array.from(files);
    const validFiles: File[] = [];
    let hasErrors = false;

    for (const file of fileArray) {
      const error = validateFile(file);
      if (error) {
        setError(error);
        hasErrors = true;
        break;
      }
      validFiles.push(file);
    }

    if (!hasErrors) {
      setError(null);
      onFileSelect(validFiles);
    }
  };

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);

    if (disabled) return;

    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFiles(e.dataTransfer.files);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    e.preventDefault();
    if (e.target.files && e.target.files[0]) {
      handleFiles(e.target.files);
    }
  };

  const openFileDialog = () => {
    if (fileInputRef.current && !disabled) {
      fileInputRef.current.click();
    }
  };

  return (
    <div style={{ width: '100%' }}>
      <div
        onClick={openFileDialog}
        onDragEnter={handleDrag}
        onDragLeave={handleDrag}
        onDragOver={handleDrag}
        onDrop={handleDrop}
        style={{
          border: dragActive ? '2px dashed #2563eb' : error ? '2px dashed #ef4444' : '2px dashed #d1d5db',
          borderRadius: 12,
          padding: 40,
          textAlign: 'center',
          backgroundColor: dragActive ? 'rgba(37, 99, 235, 0.05)' : error ? 'rgba(239, 68, 68, 0.05)' : '#fafafa',
          cursor: disabled ? 'not-allowed' : 'pointer',
          transition: 'all 0.2s ease',
          opacity: disabled ? 0.6 : 1
        }}
      >
        <input
          ref={fileInputRef}
          type="file"
          multiple={multiple}
          accept={accept}
          onChange={handleChange}
          disabled={disabled}
          style={{ display: 'none' }}
        />

        {children ? (
          children
        ) : (
          <div>
            <div style={{
              fontSize: 48,
              marginBottom: 16,
              color: dragActive ? '#2563eb' : error ? '#ef4444' : '#6b7280'
            }}>
              📁
            </div>
            
            <div style={{
              fontSize: 16,
              fontWeight: 600,
              color: '#374151',
              marginBottom: 8
            }}>
              {dragActive 
                ? 'Отпустите файлы здесь' 
                : 'Перетащите файлы сюда или нажмите для выбора'
              }
            </div>
            
            <div style={{
              fontSize: 14,
              color: '#6b7280',
              marginBottom: 16
            }}>
              {multiple 
                ? `Поддерживаются файлы до ${maxSize} МБ каждый`
                : `Поддерживается один файл до ${maxSize} МБ`
              }
            </div>

            {accept !== '*/*' && (
              <div style={{
                fontSize: 12,
                color: '#9ca3af',
                fontFamily: 'monospace'
              }}>
                Разрешенные типы: {accept}
              </div>
            )}
          </div>
        )}
      </div>

      {error && (
        <div style={{
          marginTop: 12,
          padding: 12,
          backgroundColor: '#fee2e2',
          color: '#991b1b',
          borderRadius: 6,
          fontSize: 14,
          fontWeight: 500,
          border: '1px solid #fecaca'
        }}>
          ⚠️ {error}
        </div>
      )}
    </div>
  );
};

export default FileUpload;
