# Corretora Distribuída — Java RMI

Sistema distribuído que simula uma corretora de valores utilizando **Java RMI (Remote Method Invocation)**, com suporte a múltiplos clientes simultâneos, notificações em tempo real via **callback** e reconexão automática.

---

## Estrutura do Projeto

```
corretora-distribuida-rmi/
├── src/
│   ├── ClienteCallback.java      # Interface remota de callback (implementada pelo cliente)
│   ├── CorretorInterface.java    # Interface remota da corretora (exposta pelo servidor)
│   ├── CorretorImpl.java         # Implementação do servidor (thread-safe)
│   ├── Servidor.java             # Entry point do servidor
│   └── Cliente.java              # Entry point do cliente (menu interativo)
├── out/                          # Bytecodes compilados (gerado pelo compile.bat)
├── compile.bat                   # Compila todos os fontes
├── run-servidor.bat              # Inicia o servidor
└── run-cliente.bat               # Inicia um cliente
```

---

## Pré-requisitos

- Java JDK 17 ou superior instalado
- Variável `JAVA_HOME` configurada e `java`/`javac` no PATH

---

## Como Executar

### 1. Compilar

```bat
compile.bat
```

### 2. Iniciar o Servidor

```bat
run-servidor.bat
```

O servidor iniciará na porta **1099** e pré-cadastrará as ações: `BTC`, `ETH`, `SOL`, `PETR4`, `VALE3`.

### 3. Iniciar um ou mais Clientes

Abra novos terminais e execute:

```bat
run-cliente.bat
```

> Para a demonstração, abra **dois ou mais terminais** com `run-cliente.bat` simultaneamente.

---

## Funcionalidades

### Servidor

| Operação | Descrição |
|---|---|
| `cadastrarAcao` | Cadastra nova ação com preço inicial |
| `consultarPreco` | Retorna o preço atual de uma ação |
| `listarAcoes` | Retorna todas as ações e preços |
| `atualizarPreco` | Atualiza preço e notifica clientes via callback |
| `registrarCallback` | Registra cliente para receber alertas de preço |
| `cancelarCallback` | Remove registro de alertas |

### Cliente — Menu Interativo

```
[1] Listar ações disponíveis
[2] Consultar preço de uma ação
[3] Atualizar preço de uma ação
[4] Assinar notificações de ação   ← callback em tempo real
[5] Cancelar assinatura
[6] Cadastrar nova ação
[0] Sair
```

---

## Requisitos Atendidos

| Requisito | Solução |
|---|---|
| **Concorrência** | `ConcurrentHashMap` + `synchronized` nas listas de callbacks |
| **Comunicação remota** | Java RMI padrão (Registry porta 1099) |
| **Transparência de acesso** | Cliente usa apenas a interface `CorretorInterface` |
| **Callback / tempo real** | `ClienteCallback` exportado como objeto remoto no cliente |
| **Controle de acesso** | Validação de símbolos; `RemoteException` para operações inválidas |
| **Tolerância a falhas** | Cliente tenta reconectar automaticamente (até 5 tentativas, 3s cada) |

---

## Demonstração Sugerida

1. Abra o servidor (`run-servidor.bat`)
2. Abra dois clientes em terminais separados (`run-cliente.bat`)
3. No **Cliente 1**: assine notificações para `BTC` (opção 4)
4. No **Cliente 2**: atualize o preço de `BTC` (opção 3)
5. Observe o **alerta em tempo real** aparecer no Cliente 1!
6. Teste consultas simultâneas nos dois clientes