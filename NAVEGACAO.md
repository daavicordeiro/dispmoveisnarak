# Unifor Fit — mapa de navegação e pop-ups

Abrir a pasta `UniforFit` no Android Studio (AGP 9.3.3, Gradle 9.5, Kotlin, minSdk 26).

## Contas de demonstração

| Perfil | Login | Senha |
|---|---|---|
| Aluno | R.A. `2201234567` | `12345678` |
| Gestor | `professor@unifor.br` | `12345678` (código de verificação: qualquer 6 dígitos) |
| Primeiro acesso | R.A. de aluno matriculado sem conta (ex.: `2201234569` a `2201234572`), e-mail `@edu.unifor.br` | mínimo 8 caracteres |

## Banco de dados (Firebase)

Os dados ficam no Firebase do projeto `uniforfit-unifor` (Authentication + Firestore) e são carregados no login.
Na primeira abertura o app cria a conta do gestor, os alunos matriculados, o catálogo de exercícios e treinos e
os lembretes (`data/Semeador.kt`), só se `config/seed` ainda não existir.

| Arquivo | Papel |
|---|---|
| `data/Banco.kt` | login, cadastro, leitura e gravação de tudo no Firestore |
| `data/Dados.kt` | estado da sessão em memória usado pelas telas |
| `data/Semeador.kt` | dados iniciais |
| `ui/chatbot/Assistente.kt` | assistente com Gemini (Firebase AI Logic) |
| `firestore.rules` | regras de segurança, publicadas no console do Firebase |

O assistente usa o App Check: em cada aparelho de teste, cadastre no console (App Check → Gerenciar tokens de
depuração) o token que aparece no Logcat em "Enter this debug secret into the allow list".

## Transições

| Tipo | Onde |
|---|---|
| Deslizar da direita (voltar desliza para a direita) | avanço normal no fluxo |
| Fade | splash → login, login → início, troca de abas da navbar, sair da conta |
| Subir de baixo | Assistente (chatbot) e Execução da série |
| Zoom + fade | todos os pop-ups |

Na navbar, o Início fica sempre na base da pilha: "voltar" a partir de qualquer aba leva ao Início.

## Fluxo do aluno

```
Splash → Login ─┬─ Esqueci minha senha → Recuperar senha
                ├─ Primeiro acesso → Anamnese (1/3) → PAR-Q (2/3) → Medidas (3/3) → Apresentação → Início
                ├─ Painel do Gestor → Login do gestor
                └─ Entrar → Início

Início ─ sino → Notificações
       ─ Treino de hoje → Detalhe do treino ─ ⓘ → Detalhe do exercício → Personalizar carga
                                            ─ Iniciar treino → Execução da série → (Treino concluído) → Início
       ─ Seu ranking → Ranking
Navbar: Início · Treinos (Minha semana) · Evolução (carga ⇄ corporal → Calculadora de IMC) · Ranking · Perfil
Perfil ─ Editar anamnese (Anamnese → PAR-Q → Medidas → volta ao Perfil) · Fale com seu professor · Acessibilidade · Sair
Botão ✨ (várias telas) → Assistente
Sem internet em ações de rede → Sem conexão → "Tentar novamente" repete a ação
```

Estados vazios: domingo da Minha semana → "Nenhum treino atribuído"; aba Puxada da Evolução → "Sem dados ainda"; "Limpar" nas Notificações → "Nenhum aviso por enquanto".

## Fluxo do gestor

```
Login do gestor → Painel ─ Alunos ─ busca / + → Editar aluno
                         │        └ aluno → Ficha (Anamnese · PAR-Q · Medidas · Evolução) → Editar aluno
                         ─ Treinos → Editar treino
                         ─ Exercícios (filtros) → Editar exercício
                         ─ Lembretes
                         ─ avatar → Perfil do gestor ─ Alunos atribuídos a mim · Nomenclatura (Exercícios)
                                                     ─ Acessibilidade · Sair
```

## Pop-ups

| Tela | Pop-up | Por quê |
|---|---|---|
| Primeiro acesso | Termos de uso e privacidade | RNF04.2 — LGPD no primeiro acesso |
| Primeiro acesso | "Você já tem conta" | R.A. já cadastrado |
| Recuperar senha | "Link enviado!" (e-mail mascarado) | confirma o envio (RF04) |
| PAR-Q | "Atenção à sua saúde" | alguma resposta "Sim" (RF06.2) |
| Medidas (modo edição) | "Dados atualizados!" | volta ao Perfil (RF07.2) |
| Início | "Treino de hoje concluído" | evita repetir o treino sem querer |
| Minha semana | "Dia de descanso" | dia sem treino com orientação |
| Detalhe do treino | "Iniciar este treino?" | treino planejado para outro dia ou já feito |
| Personalizar carga | "Carga salva!" / "Novo recorde! 🎉" | confirma RF13.3 |
| Execução da série | "Descanso · 00:59" com "Pular descanso" | intervalo entre séries (RF14.3) |
| Execução da série | "Encerrar treino?" | evita perder a sessão |
| Execução da série | "Treino concluído!" | RF15 (já previsto no protótipo) |
| Evolução corporal / Editar exercício / Perfil | Câmera ou galeria (bottom sheet) | RF17.3 / RF38.5 |
| Notificações | Aviso completo + atalho para a tela relacionada | marca como lida (RF20.2) |
| Notificações | "Limpar notificações?" | ação em lote |
| Fale com seu professor | "Mensagem enviada" e "Abrir o WhatsApp?" | RF22.2 / RF22.3 (sai do app) |
| Perfil (aluno e gestor) | "Sair da conta?" | RF21.5 / RF40.5 |
| Login do gestor | "Acesso restrito à equipe" | e-mail de aluno (RF27.2) |
| Login do gestor | Verificação em duas etapas | RNF05.3 |
| Ficha do aluno | Solicitar novo PAR-Q · Anexo assinado · Histórico de medições | RF31.7.2 / RF31.7.3 / RF31.8.3 |
| Editar aluno | "Excluir aluno?" | ação destrutiva (RF30.5) |
| Editar treino | "Remover exercício?" · "Adicionar exercícios" · "Salvar e enviar aos alunos?" | RF36.3 / RF36.4 / RF36.5 |
| Editar aluno/treino/exercício | "Descartar alterações?" | voltar com formulário alterado |
| Lembretes | "Nova regra de lembrete" | RF39.4 |
| Perfil do gestor | "Meus dados" · "Notificações do painel" | RF40.1 / RF40.3 |

Avisos simples (lembrete ligado/desligado, aluno salvo, foto adicionada) usam a barra de aviso no rodapé (Snackbar) em vez de pop-up.

## Onde está cada coisa

- `core/BaseActivity.kt` — transições, navbar, botão ✨, links "← Voltar", tela Sem conexão, tamanho da fonte
- `core/Popup.kt` + `res/layout/popup_padrao.xml` — componente único de pop-up
- `core/Transicao.kt` + `res/anim/` — animações de troca de tela
- `data/Dados.kt` — dados de demonstração
- `ui/…` — telas do aluno · `gestor/…` — telas do gestor
