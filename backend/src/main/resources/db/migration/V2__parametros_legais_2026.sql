-- V2 — Valores vigentes em 2026.
--
-- CONFERIR no Portal do Simples Nacional antes de cobrar do primeiro cliente: as
-- fontes de 2026 divergem no centavo. Errar aqui custa um UPDATE, nunca um deploy.
--
-- Falta de proposito: mei.das.transporte (MEI caminhoneiro, INSS de 12% em vez de 5%).
-- Sem valor confirmado, preferimos que a geracao de obrigacao falhe alto a semear
-- um numero inventado. Inserir quando aparecer o primeiro cliente de transporte.

insert into legal_parameter (param_key, valid_from, valid_to, value, source_note) values
    ('mei.annual_revenue_limit', '2026-01-01', null, '{"cents": 8100000}',
     'R$ 81.000,00 — inalterado desde 2018'),

    ('mei.tolerance_pct', '2026-01-01', null, '{"pct": 20}',
     'Ate 20% acima do teto (R$ 97.200) o MEI segue no regime pagando DAS complementar'),

    ('mei.das.comercio', '2026-01-01', null, '{"cents": 8205}',
     'Comercio e industria — CONFERIR no Portal do Simples Nacional'),
    ('mei.das.servicos', '2026-01-01', null, '{"cents": 8605}',
     'Prestacao de servicos — CONFERIR no Portal do Simples Nacional'),
    ('mei.das.comercio_servicos', '2026-01-01', null, '{"cents": 8705}',
     'Comercio e servicos juntos — CONFERIR no Portal do Simples Nacional'),

    ('mei.das.due_day', '2026-01-01', null, '{"day": 20}',
     'Vencimento do DAS: dia 20 do mes seguinte'),
    ('mei.dasn.due_date', '2026-01-01', null, '{"month": 5, "day": 31}',
     'DASN-SIMEI: declaracao anual, 31 de maio'),

    ('mei.proportional_first_year', '2026-01-01', null, '{"enabled": true}',
     'No ano de abertura o limite e 81.000 / 12 x meses de atividade'),
    ('mei.alert_thresholds', '2026-01-01', null, '{"pct": [50, 75, 90, 100]}',
     'Niveis do alerta progressivo de faturamento, em % do teto');
