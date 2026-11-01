# Árvore Binária de Busca Parcialmente Persistente — Java

**Alunos: Maria Elanne Mendes Rodrigues (603463) 
Vagner Henrique (603486)**

Solução para o **trabalho de persistência parcial (pós-graduação)**,implementar uma árvore binária de busca da disciplina de Estruturas de Dados (CKP8077),

**Linguagem:** Java 

**Dependências:** somente a biblioteca padrão do Java.  
**Arquivos:** `Principal.java` (toda a implementação), `Makefile` (compilação e execução), `entrada.in` (exemplo), `saida_esperada.out` (referência para o exemplo).

## Ideia central

Em vez de copiar toda a árvore a cada atualização, cada nó guarda seus campos originais (`valor`, `esquerda`, `direita`) e **uma única área para uma alteração histórica** (campo, nova informação e versão da mudança).

1. Quando um campo muda e essa área está **livre**, a alteração é registrada ali, sem copiar o nó.
2. Quando a área está **ocupada**, fazemos uma cópia do nó com os campos mais recentes, aplicamos a nova alteração na cópia e redirecionamos a ligação do pai para ela.
3. Se a alteração do pai também exigir uma cópia, o processo continua em direção à raiz.
4. Um vetor `raizes[0..1000]` guarda a raiz de cada versão publicada, inclusive a versão zero, vazia.
5. Cada consulta em uma versão histórica lê os campos originais ou a alteração, conforme o número da versão. Uma consulta nunca modifica a estrutura.

Os ponteiros `pai` são **auxiliares e exclusivos da árvore atual**: podem ser atualizados livremente, pois consultas antigas percorrem somente as ligações `esquerda` e `direita`, não o campo `pai`.

Essa abordagem utiliza a técnica de cópia de nós com caixa de modificação (node copying with modification box), descrita por Driscoll, Sarnak, Sleator e Tarjan. O artigo de 2026 sobre zip-zip trees também discute sua aplicação à persistência parcial em árvores binárias de busca. Optamos por uma BST sem balanceamento, conforme permitido pelo enunciado. Assim, as operações de busca, inserção e remoção dependem da altura da árvore, que pode chegar a O(n), enquanto a sobrecarga adicional da persistência é analisada amortizadamente.

## Complexidade

Sejam `v` o número de versões criadas, `n` a quantidade de nós de uma versão e `h` a altura da árvore.

| Operação | Tempo | Espaço **persistente** adicional |
|---|---|---|
| `INC N` | `O(h)` amortizado | `O(1)` amortizado |
| `REM N` | `O(h)` amortizado | `O(1)` amortizado |
| `SUC N M` | `O(h)` | `O(1)` |
| `IMP M` | `O(n)` | `O(h)` auxiliar temporário (pilha) |
| Armazenamento de todas as versões | — | **`O(v)` total** |

A busca, a inserção, a remoção e a consulta de sucessor percorrem, no máximo, caminhos da árvore, com custo básico O(h). A impressão percorre os n nós em ordem, com custo O(n) e espaço auxiliar O(h) para a pilha.

Análise amortizada da persistência. Seja Φ o número de nós da árvore atual cujas caixas de modificação estão ocupadas. Quando uma alteração copia um nó com caixa ocupada, a cópia passa a integrar a árvore atual com a caixa livre, enquanto o nó original é preservado para as versões históricas.

Se uma propagação copia k nós, seu custo é O(k + 1) e a variação do potencial satisfaz ΔΦ ≤ 1 − k. Utilizando um potencial escalado Ψ = CΦ, com uma constante C suficientemente grande, o custo amortizado é O(1) por campo modificado.

Cada inclusão modifica um número constante de campos, assim como cada remoção, inclusive no caso de nós com dois filhos. Portanto, a sobrecarga amortizada da persistência é O(1) por atualização, além do custo básico de percorrer a árvore.

Espaço persistente total. Cada inclusão cria um nó, e o número total de cópias ao longo das atualizações é O(v), pelo argumento amortizado. Como o vetor de raízes também ocupa O(v) espaço, o espaço persistente total é O(v). A pilha utilizada por IMP é temporária e ocupa até O(h) espaço auxiliar.


## Contrato das operações

- `INC N`: insere `N`, inclusive se repetido, e cria uma nova versão.

- `REM N`: remove **uma** ocorrência e cria uma nova versão, mesmo se `N` não existir.

- `SUC N M`: mostra a linha de comando e, abaixo, o menor elemento **estritamente maior** que `N` na versão `M`; imprime `inf` se não houver sucessor.

- `IMP M`: mostra a linha de comando e, abaixo, os elementos da versão `M` em ordem crescente, separados por `, `; para uma árvore vazia imprime uma linha vazia. Não cria versão.

- A versão inicial é `0`. No máximo 1000 operações `INC` e `REM` criam as versões de `1` a `1000`.

- Para `IMP` com versão inexistente, consulta-se a versão atual, como determina o enunciado. **Para `SUC` com versão inexistente**, não há regra explícita; adotamos por consistência o mesmo comportamento, embora os testes devam usar versões existentes.

- Valores armazenados são inteiros não negativos de até 64 bits com sinal (`long`), abrangendo confortavelmente os 16 bits mínimos pedidos.

- As únicas saídas do programa são as das consultas `IMP` e `SUC`, preservando a formatação pedida.

## Compilação e execução no Linux

```bash
make build
make run INPUT=entrada.in
```

A regra `run` também compila, caso `build` ainda não tenha sido executada, e **não exibe** os comandos de compilação na saída. A saída é impressa no terminal. Para comparar exatamente com a referência:

```bash
make run INPUT=entrada.in > saida_obtida.out
diff -u saida_esperada.out saida_obtida.out
```

Para limpar as classes compiladas:

```bash
make clean
```

A execução direta também é possível:

```bash
javac --release 17 -encoding UTF-8 Principal.java
java Principal entrada.in
```

Se nenhum arquivo for informado na execução direta, o programa lê da entrada padrão.

## Organização do código — `Principal.java`

| Classe ou função | O que faz |
|---|---|
| `Principal` | Contém a leitura de entrada e as duas classes auxiliares. |
| `No` | Guarda valor, filhos, pai da versão atual e uma alteração com seu identificador de versão. |
| `ArvorePersistente` | Mantém as raízes históricas, a raiz atual e o contador de versões. |
| `obterValor` | Consulta o valor de um nó em determinada versão. |
| `obterEsquerda` | Consulta o filho esquerdo de um nó em determinada versão. |
| `obterDireita` | Consulta o filho direito de um nó em determinada versão. |
| `alterarCampo` | Registra uma alteração ou cria e encadeia cópias de nós quando necessário. |
| `publicarVersao` | Registra uma nova versão da raiz, mesmo quando a remoção não encontra o valor. |
| `buscarAtual` | Procura um nó com a chave informada na versão mais recente. |
| `incluir` | Insere uma ocorrência na BST e publica uma versão. |
| `substituirNo` | Faz a ligação do pai apontar para o filho que substitui um nó retirado. |
| `remover` | Remove uma ocorrência, tratando os casos de zero, um ou dois filhos. |
| `resolverVersao` | Seleciona a raiz histórica apropriada ou a versão mais recente. |
| `sucessor` | Procura a menor chave estritamente maior que a informada. |
| `imprimir` | Percorre iterativamente a BST em ordem na versão solicitada. |
| `conferirCapacidade` | Verifica o máximo de mil atualizações. |
| `executar` | Lê e interpreta `INC`, `REM`, `SUC`, `IMP`, escrevendo apenas as saídas requeridas. |
| `validarValorInserido` | Rejeita valores negativos em inserções e remoções. |
| `main` | Abre o arquivo fornecido ou utiliza entrada padrão. (`main` é o nome obrigatório do ponto de entrada Java.) |

## Referências acadêmicas

1. **Driscoll, J. R.; Sarnak, N.; Sleator, D. D.; Tarjan, R. E.** *Making Data Structures Persistent*. Journal of Computer and System Sciences, 38(1), p. 86–124, 1989. DOI: https://doi.org/10.1016/0022-0000(89)90034-2. Base teórica da cópia de nós com espaço e sobrecarga amortizados constantes.
2. **Gila, O.; Goodrich, M. T.; Tarjan, R. E.** *Zip-zip Trees: Making Zip Trees More Balanced, Biased, Compact, or Persistent*. Algorithmica, 88, artigo 40, 2026. DOI: https://doi.org/10.1007/s00453-025-01364-2. Referência moderna que mostra a relevância da técnica para árvores parcialmente persistentes.
3. **MIT, 6.854 — Advanced Algorithms.** *Lecture 2: Persistent Data Structures*. https://courses.csail.mit.edu/6.854/18/Scribe/s2-persistent/s2-persistent.html. Descreve o uso das áreas de modificação e a análise por potencial.

## Testes

Os arquivos `entrada.in` e `saida_esperada.out` fornecem um teste mínimo de regressão que acompanha a entrega.
