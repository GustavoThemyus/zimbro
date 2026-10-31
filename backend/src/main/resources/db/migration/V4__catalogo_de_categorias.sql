-- V4 — Catalogo de fabrica. workspace_id nulo: vale para todos os clientes.
-- Poucas categorias de proposito. Lista comprida faz o usuario parar para
-- pensar, e o lancamento rapido tem que caber em tres toques.

insert into category (workspace_id, name, slug, kind, default_scope, color_token, is_system, sort_order) values
    -- entradas do negocio
    (null, 'Vendas',                  'vendas',            'INCOME',     'BUSINESS', 'cat-teal',   true, 10),
    (null, 'Serviços prestados',      'servicos',          'INCOME',     'BUSINESS', 'cat-teal',   true, 20),
    (null, 'Outras entradas',         'outras-entradas',   'INCOME',     'BUSINESS', 'cat-teal',   true, 30),

    -- saidas do negocio
    (null, 'Insumos e material',      'insumos',           'EXPENSE',    'BUSINESS', 'cat-lime',   true, 100),
    (null, 'Mercadoria pra revenda',  'mercadoria',        'EXPENSE',    'BUSINESS', 'cat-lime',   true, 110),
    (null, 'Aluguel',                 'aluguel',           'EXPENSE',    'BUSINESS', 'cat-blue',   true, 120),
    (null, 'Luz, água e internet',    'contas-fixas',      'EXPENSE',    'BUSINESS', 'cat-blue',   true, 130),
    (null, 'Taxa de maquininha',      'taxa-maquininha',   'EXPENSE',    'BUSINESS', 'cat-blue',   true, 140),
    (null, 'Transporte e combustível','transporte',        'EXPENSE',    'BUSINESS', 'cat-lime',   true, 150),
    (null, 'Divulgação',              'divulgacao',        'EXPENSE',    'BUSINESS', 'cat-violet', true, 160),
    (null, 'Equipamento e ferramenta','equipamento',       'EXPENSE',    'BUSINESS', 'cat-lime',   true, 170),
    (null, 'Manutenção',              'manutencao',        'EXPENSE',    'BUSINESS', 'cat-lime',   true, 180),
    (null, 'Assinaturas e apps',      'assinaturas',       'EXPENSE',    'BUSINESS', 'cat-violet', true, 190),
    (null, 'Ajudante',                'ajudante',          'EXPENSE',    'BUSINESS', 'cat-violet', true, 200),
    (null, 'Outras saídas',           'outras-saidas',     'EXPENSE',    'BUSINESS', 'cat-slate',  true, 210),

    -- pessoal
    (null, 'Mercado',                 'mercado',           'EXPENSE',    'PERSONAL', 'cat-rose',   true, 300),
    (null, 'Casa',                    'casa',              'EXPENSE',    'PERSONAL', 'cat-rose',   true, 310),
    (null, 'Saúde',                   'saude',             'EXPENSE',    'PERSONAL', 'cat-rose',   true, 320),
    (null, 'Lazer',                   'lazer',             'EXPENSE',    'PERSONAL', 'cat-rose',   true, 330),
    (null, 'Outros pessoais',         'pessoal-outros',    'EXPENSE',    'PERSONAL', 'cat-rose',   true, 340),

    -- os tres que fazem o fechamento ser correto
    (null, 'Retirada pra mim',        'retirada',          'OWNER_DRAW', 'BUSINESS', 'cat-violet', true, 400),
    (null, 'DAS',                     'das',               'TAX',        'BUSINESS', 'cat-slate',  true, 410),
    (null, 'Transferência',           'transferencia',     'TRANSFER',   'MIXED',    'cat-slate',  true, 420);
