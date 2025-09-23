import React, { useState, useEffect } from 'react';
import { CertificateTemplate } from '../models/certificateTemplate';
import templatesApi from '../api/certificates/templatesApi';
import caApi from '../api/certificates/caApi';

const CertificateTemplates: React.FC = () => {
  const [templates, setTemplates] = useState<CertificateTemplate[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCreateForm, setShowCreateForm] = useState(false);

  useEffect(() => {
    loadTemplates();
  }, []);

  const loadTemplates = async () => {
    try {
      setLoading(true);
      setError(null);
      const templateList = await templatesApi.getAllTemplates();
      setTemplates(templateList);
    } catch (err) {
      setError('Ошибка при загрузке шаблонов');
      console.error('Error loading templates:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteTemplate = async (id: number) => {
    if (!confirm('Вы уверены, что хотите удалить этот шаблон?')) return;

    try {
      await templatesApi.deleteTemplate(id);
      setTemplates(prev => prev.filter(t => t.id !== id));
      alert('Шаблон успешно удален');
    } catch (err) {
      setError(`Ошибка при удалении шаблона: ${err instanceof Error ? err.message : 'Неизвестная ошибка'}`);
      console.error('Error deleting template:', err);
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('ru-RU');
  };

  if (loading) {
    return (
      <div style={{ padding: 32, textAlign: 'center' }}>
        <h1>Шаблоны сертификатов</h1>
        <div>Загрузка...</div>
      </div>
    );
  }

  return (
    <div style={{ padding: 32 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <h1 style={{ margin: 0 }}>Шаблоны сертификатов</h1>
        <div>
          <button
            onClick={loadTemplates}
            style={{
              padding: '8px 16px',
              backgroundColor: '#10b981',
              color: 'white',
              border: 'none',
              borderRadius: '6px',
              cursor: 'pointer',
              marginRight: 8
            }}
          >
            Обновить
          </button>
          <button
            onClick={() => setShowCreateForm(!showCreateForm)}
            style={{
              padding: '8px 16px',
              backgroundColor: '#3b82f6',
              color: 'white',
              border: 'none',
              borderRadius: '6px',
              cursor: 'pointer'
            }}
          >
            {showCreateForm ? 'Отмена' : 'Создать шаблон'}
          </button>
        </div>
      </div>

      {error && (
        <div style={{
          backgroundColor: '#fee2e2',
          color: '#991b1b',
          padding: 16,
          borderRadius: 6,
          marginBottom: 24,
          border: '1px solid #fecaca'
        }}>
          {error}
        </div>
      )}

      {showCreateForm && (
        <CreateTemplateForm 
          onSuccess={() => {
            setShowCreateForm(false);
            loadTemplates();
          }}
          onCancel={() => setShowCreateForm(false)}
        />
      )}

      {templates.length === 0 ? (
        <div style={{ 
          textAlign: 'center', 
          padding: 40,
          backgroundColor: '#f9fafb',
          borderRadius: 8,
          color: '#6b7280'
        }}>
          <p>Шаблоны сертификатов не найдены</p>
          <p>Создайте шаблон для упрощения выпуска сертификатов</p>
        </div>
      ) : (
        <div style={{ backgroundColor: 'white', borderRadius: 8, boxShadow: '0 2px 8px #e0e7ff' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead style={{ backgroundColor: '#f1f5f9' }}>
              <tr>
                <th style={{ padding: 12, textAlign: 'left' }}>Название</th>
                <th style={{ padding: 12, textAlign: 'left' }}>ЦА-издатель</th>
                <th style={{ padding: 12, textAlign: 'left' }}>Макс. срок (дни)</th>
                <th style={{ padding: 12, textAlign: 'left' }}>Создан</th>
                <th style={{ padding: 12, textAlign: 'left' }}>Владелец</th>
                <th style={{ padding: 12, textAlign: 'left' }}>Действия</th>
              </tr>
            </thead>
            <tbody>
              {templates.map((template) => (
                <tr key={template.id} style={{ borderBottom: '1px solid #e5e7eb' }}>
                  <td style={{ padding: 12, fontWeight: 'bold' }}>{template.name}</td>
                  <td style={{ padding: 12 }}>{template.caIssuer.subject}</td>
                  <td style={{ padding: 12 }}>{template.maxTtlDays || 'Не задано'}</td>
                  <td style={{ padding: 12 }}>{formatDate(template.createdAt)}</td>
                  <td style={{ padding: 12 }}>{template.owner.name} {template.owner.surname}</td>
                  <td style={{ padding: 12 }}>
                    <button 
                      onClick={() => handleDeleteTemplate(template.id)}
                      style={{ 
                        padding: '4px 12px',
                        fontSize: '12px',
                        backgroundColor: '#ef4444',
                        color: 'white',
                        border: 'none',
                        borderRadius: '4px',
                        cursor: 'pointer'
                      }}
                    >
                      Удалить
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

// Простая форма создания шаблона
const CreateTemplateForm: React.FC<{
  onSuccess: () => void;
  onCancel: () => void;
}> = ({ onSuccess, onCancel }) => {
  const [name, setName] = useState('');
  const [maxTtlDays, setMaxTtlDays] = useState('');
  const [cnRegex, setCnRegex] = useState('');
  const [sanRegex, setSanRegex] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!name.trim()) {
      setError('Название шаблона обязательно');
      return;
    }

    try {
      setLoading(true);
      setError(null);

      // Для простоты создаем шаблон без привязки к конкретному ЦА
      // В реальном приложении нужно будет выбирать ЦА из списка
      await templatesApi.createTemplate({
        name: name.trim(),
        maxTtlDays: maxTtlDays ? parseInt(maxTtlDays) : undefined,
        cnRegex: cnRegex.trim() || undefined,
        sanRegex: sanRegex.trim() || undefined,
      } as any); // TODO: доделать интерфейс для создания

      alert('Шаблон успешно создан');
      onSuccess();
    } catch (err) {
      setError(`Ошибка при создании шаблона: ${err instanceof Error ? err.message : 'Неизвестная ошибка'}`);
      console.error('Error creating template:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ 
      backgroundColor: 'white', 
      padding: 24, 
      borderRadius: 8, 
      boxShadow: '0 2px 8px #e0e7ff',
      marginBottom: 24 
    }}>
      <h3 style={{ marginTop: 0 }}>Создать новый шаблон</h3>
      
      {error && (
        <div style={{
          backgroundColor: '#fee2e2',
          color: '#991b1b',
          padding: 12,
          borderRadius: 6,
          marginBottom: 16,
          border: '1px solid #fecaca'
        }}>
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: 16 }}>
          <label style={{ display: 'block', marginBottom: 4, fontWeight: 'bold' }}>
            Название шаблона *
          </label>
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            style={{
              width: '100%',
              padding: 8,
              border: '1px solid #d1d5db',
              borderRadius: 4
            }}
            placeholder="Например: Стандартный пользователь"
          />
        </div>

        <div style={{ marginBottom: 16 }}>
          <label style={{ display: 'block', marginBottom: 4, fontWeight: 'bold' }}>
            Максимальный срок действия (дни)
          </label>
          <input
            type="number"
            value={maxTtlDays}
            onChange={(e) => setMaxTtlDays(e.target.value)}
            style={{
              width: '100%',
              padding: 8,
              border: '1px solid #d1d5db',
              borderRadius: 4
            }}
            placeholder="365"
          />
        </div>

        <div style={{ marginBottom: 16 }}>
          <label style={{ display: 'block', marginBottom: 4, fontWeight: 'bold' }}>
            Регулярное выражение для CN
          </label>
          <input
            type="text"
            value={cnRegex}
            onChange={(e) => setCnRegex(e.target.value)}
            style={{
              width: '100%',
              padding: 8,
              border: '1px solid #d1d5db',
              borderRadius: 4
            }}
            placeholder="^[a-zA-Z0-9\s]+$"
          />
        </div>

        <div style={{ marginBottom: 20 }}>
          <label style={{ display: 'block', marginBottom: 4, fontWeight: 'bold' }}>
            Регулярное выражение для SAN
          </label>
          <input
            type="text"
            value={sanRegex}
            onChange={(e) => setSanRegex(e.target.value)}
            style={{
              width: '100%',
              padding: 8,
              border: '1px solid #d1d5db',
              borderRadius: 4
            }}
            placeholder="^[a-zA-Z0-9.-]+$"
          />
        </div>

        <div style={{ display: 'flex', gap: 8 }}>
          <button
            type="submit"
            disabled={loading}
            style={{
              padding: '8px 16px',
              backgroundColor: loading ? '#9ca3af' : '#10b981',
              color: 'white',
              border: 'none',
              borderRadius: '6px',
              cursor: loading ? 'not-allowed' : 'pointer'
            }}
          >
            {loading ? 'Создается...' : 'Создать'}
          </button>
          <button
            type="button"
            onClick={onCancel}
            style={{
              padding: '8px 16px',
              backgroundColor: '#6b7280',
              color: 'white',
              border: 'none',
              borderRadius: '6px',
              cursor: 'pointer'
            }}
          >
            Отмена
          </button>
        </div>
      </form>
    </div>
  );
};

export default CertificateTemplates;
