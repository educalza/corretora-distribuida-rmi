# Corretora Distribuída RMI

Este projeto consiste em um sistema de corretora de ativos distribuído utilizando **Java RMI (Remote Method Invocation)**. Ele permite que múltiplos clientes se conectem a um servidor central para listar ativos, consultar preços e realizar operações de compra e venda com atualizações em tempo real.

## 🚀 Funcionalidades

- **Listagem de Ativos:** Visualização de todos os ativos disponíveis (ações, criptos, etc).
- **Consulta de Preços:** Verificação do valor atual de um ativo específico.
- **Compra e Venda:** 
  - Compra incrementa o valor do ativo em 5%.
  - Venda decrementa o valor do ativo em 5%.
- **Notificações em Tempo Real (Callbacks):** Quando um cliente altera o preço de um ativo, todos os outros clientes conectados recebem uma notificação instantânea.
- **Reconexão Automática:** Se o servidor cair, o cliente entra em modo de espera e se reconecta automaticamente assim que o servidor voltar a ficar online.
- **Interface Colorida:**
  - `VERDE`: Confirmação de compra.
  - `AMARELO`: Confirmação de venda.
  - `VERMELHO`: Alertas de erro e perda de conexão.

## 📋 Pré-requisitos

- **Java JDK 21** ou superior instalado.
- Terminal compatível com **ANSI escape codes** (VS Code, Windows Terminal, PowerShell, Linux/Mac terminal) para visualizar as cores.

## 🛠️ Como Compilar

No diretório raiz do projeto, execute o comando abaixo para compilar todos os arquivos para a pasta `out`:

```bash
javac -d out src/*.java
```

## 🏃 Como Executar

### 1. Iniciar o Servidor
O servidor cria o registro RMI na porta padrão (1099) e publica o serviço.

```bash
java -cp out Servidor
```

### 2. Iniciar o Cliente
Você pode abrir múltiplos terminais para simular diferentes usuários.

```bash
java -cp out Cliente
```

## 🏗️ Estrutura do Projeto

- `src/Ativo.java`: Modelo de dados dos ativos.
- `src/CorretorRemote.java`: Interface RMI com os serviços disponíveis.
- `src/CorretorImpl.java`: Implementação lógica do servidor e gerenciamento de notificações.
- `src/Servidor.java`: Classe principal que inicia o serviço RMI.
- `src/ClienteCallback.java`: Interface para o servidor enviar notificações ao cliente.
- `src/Cliente.java`: Interface de linha de comando para o usuário.

---
Projeto desenvolvido para a disciplina de Sistemas Distribuídos.
