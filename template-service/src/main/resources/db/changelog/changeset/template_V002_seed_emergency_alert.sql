INSERT INTO templates (template_name, description, channel, content, created_by)
VALUES (
    'emergency-alert',
    'Emergency alert broadcast template',
    'EMAIL',
    '⚠️ Emergency alert
{{title}}
{{description}}
Location: {{city}}, {{street}}',
    '00000000-0000-0000-0000-000000000001'
) ON CONFLICT (template_name) DO NOTHING;
