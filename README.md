# Software is Language Too!
We present the Software Language Processing (SLP) library + CLI, here to enable fluent modeling of evolving text! This public library and command-line tool was created as part of a submission to the FSE 2017 conference<sup>1</sup>; instructions on how to cite will follow soon.

# Table of Contents
1. [Why Model Code?](#why)
2. [FAQ: How does it work?](#faq)
3. [Getting Started](#started)
4. [Release Notes](#release)

<a name="why"></a>## Why Model Code?
Code is a kind of language, and language models are awesomely useful (think of Siri, auto-correct, chatbots; all powered by language models). In code, we are beginning to discover the benefits too: good language models enable great code completion, could actually help hunt down bugs, suggest better variable names and even help you get your pull requests accepted!

But code is also very different from natural languages like English in some crucial ways: it is dynamic, rapidly evolving with every commit, adding new identifiers, methods, modules and even entire new libraries. It is also deeply hierarchical, with files in packages in modules in eco-systems, etc. Traditional language models are too static to deal with this, so we present SLP: a dynamic, scalable language modeling library.
We don't just enable fluent language model mixing and updates, we boost modeling accuracy by up to a factor 4, outperforming some LSTMs, and getting even better when mixing with them!

<a name="faq"></a># FAQ: How does it work?
Let's start with an example: imagine you are working on a web-app, and you open a file called "BillCustomer.java" in a package "Billing", with a neighboring package "Shipping". The moment you open that file, you'd like an accurate language model there for code completion, style suggestions, bug detection, you name it. The best model is one that <em>really gets</em> the context that you are working in, but what is that context? Firstly, the file itself of course; it's as specific as it gets. But it's not a lot of data for a good model; let's add all the other files in "Billing" as a second tier. After that, maybe all the files in "Shipping", then the rest of your system, and finally maybe all the other files in the world. Just like that, you have a five-tiered language model, from local to global, from specific and small, to general and huge.

But normal language models can't do this! They are usually trained once, often with limited dynamic updating, and they don't mix well either. We take a different route: we create models <em>on-demand</em> for every level of context, and wrote an algorithm that mixes all those models so that local ones are prioritized. That doesn't stop you from mixing with static models either: we actually found that mixing with an LSTM can further boost performance and hope to integrate these right into the library soon.

## So how exactly does the nested model get its probabilities?
The nested model (`-n` flag on the Jar) produces models on the fly, one for each locality, and mixes them in such a way that the most local model (often a cache on the file itself) gets the final say. What that comes down to is this (example calculation below): when given a sequence, every model first takes that sequence and computes two things: a probability <em>p</em> (calculated directly from the counts using MLE), and a confidence <em>λ</em> in that probability (which comes from the <em>smoothing</em> method used<sup>2</sup>). The confidence is the model telling you: how much information did I have for my statement? For n-grams, and especially Jelinek-Mercer smoothing, that often comes down to: what was the longest context I've seen? If that context is: `Bill bill = customer.`, you (and our local model) can probably guess that the next token is something like `getBill`. The local models would have seen this whole context, so their <em>λ</em> will be high, and our most local model will probably also predict `getBill` with a high probability, for instance if it's been used in the same file. Our global model might never even have seen `customer.` so it reports low confidence and leaves the local models to get the biggest say in their probabilities. On the other hand, the global model may have seen those rare patterns that have never been seen locally, so it's definitely helpful.

Let's visualize the probability calculation for the example above (<em>p(`Bill bill = customer.getBill`</em>)) in a table with some (made-up but not unrealistic) values for <em>p</em> and <em>λ</em>. For instance, the Cache may get its 90% probability (p=0.9) by seeing `getBill` follow the context `Bill bill = customer.` 9 out of 10 times in the current file, and may get its 95% confidence (λ=0.95) from its smoothing method that is 95% sure in any context that it has seen 10 times. These <em>p</em> and <em>λ</em> values are then mixed from global to local (here from left to right):

| Model     | <em>p / λ</em>  | Merge Global/Shipping | Merge Shipping/Billing | Merge Billing/Cache
| ----------|-----------------|------------|--------------|-------------|
| Global    | 0.05 / 0.1      |            |              |             |
| Shipping  | 0.15 / 0.5      | 0.13 / 0.5 |              |             |
| Billing   | 0.70  / 0.875   |            | 0.49 / 0.875 |             |
| Cache     | 0.90  / 0.95    |            |              | 0.70 / 0.95 |

As you can see, the most local model gets the final say and that works out well: it's as confident as it is accurate. Even so, it doesn't get the only say (we lose some probability from the cache), which is okay: the more global models "pay it back" at times where it is uncertain in turn.

## How do you update these models without flooding my memory?
The key to efficiency in our many-models approach is that every model is count-based and estimates probabilities from those counts at run-time. So it can act like a regular language model, but allow updates to these counts at will. We can't do bit-twiddling here (like the excellent KenLM) since our model must always be ready for updates. To make it all feasible, each model internally uses a <em>Trie</em> data-structure, which provides one of the most space-efficient solutions while also allowing rapid updates. We added some performance enhancements, like storing unique events as simple arrays. Plus, our newest giga-counter reduces overhead from Java's GC by storing lots of small counters while training.

## When else would I update my model?
Lots of reasons! Any time you update a file, pull a new commit, switch to another package, your development context changes. We found that software developers are really good at grouping similar files in modules, packages, etc., and we want to reward that by teaching our models to capture exactly that structure.

<a name="started"></a># Getting Started
This core module contains everything needed to get started with modeling language, including code to lex files, build vocabularies, construct (complex) models and run those models. Other modules relying on Core for more exotic applications are forthcoming.

## Code API
To get started with the code as a library, either add the Jar to your dependencies (Maven dependency coming soon!) or download the whole project and link it to yours. Have a look at `slp.core.example` to see how you would do all the setup for a natural language (NLRunner) and a Java code (JavaRunner) example; it show-cases quite a few options that you can set. The usual process takes about five steps:
1. Set up the LexerRunner with options such as whether to add delimiters around lines, or whole files, which lexer to use (e.g. preserve punctuation, split on whitespace?)
2. Set up the vocabulary by building it before-hand with some cut-off for infrequent words, or you could leave it open entirely (as turns out to be better for source code)
3. Set up the ModelRunner with options for modeling, such as whether to treat each line as a sentence (vs. the whole file, e.g. for Java), what order n-grams to use.
4. Set up a Model, e.g. a simple n-gram model, a model with cache, a mixture of global, local + cache, or an automatically nested model, maybe make it dynamic to learn every token right after modeling it.
5. Run your model on whatever data you have. You can call `ModelRunner.model` (or `.predict`) to model any sequence, file, or whole directory (recursively).
What if you don't want to model just once? Maybe you want to model every commit in a project's history and then update with that commit right after modeling it? Easy! Because all the models are count-based, you can just wrap the modeling step in a loop and alternate with calls to `ModelRunner.learn` or `ModelRunner.forget` to keep your model up-to-date without retraining the whole thing.

## Command Line API
The code ships as a Java library and comes with a JAR (use v0.2) that serves as a command line API. To get started with the command line API, simply download the Jar and type `java -jar SLP-Core_v0.2.jar --help` to get your code modeling adventures started!

**A very quick start:** running a 6-gram, JM cache model on train and test Java directories:  
`java -jar SLP-Core_v0.2.jar train-test --train train-path --test test-path --language java --cache`

Based on the two example files (NLRunner, JavaRunner) in the library API as mentioned above, here are the same steps using the command-line API, a tad verbose to show all the options. Here, I presume some 'train' and 'test' directory with your data.

**A typical natural language use-case:**  
(think: closed vocabularies, modeling each line separately, etc).  
- First some common flags for lexing: `-l simple` tells the Jar to use the `simple` (default) lexer which splits on whitespace and punctuation, preserving punctuation as a separate token, the `--delims` flag tells it to add a start and end-of-sentence markers. 
- Create a vocabulary for the train data, throwing away all words seen only once, and write to `train.vocab`:  
&nbsp;&nbsp;&nbsp;&nbsp;`java -jar SLP-Core_v0.2.jar vocabulary train train.vocab --unk-cutoff 2 -l simple --delims`  
  * `vocabulary` mode to create the vocabulary, takes two positional arguments: in-directory and out-file path
  * `--unk-cutoff` (or `-u`) sets the minimum number of times seen to be preserved
  * `-l` sets the lexer (also used below), `simple` refers to the default lexer that splits on both whitespace and punctuation and preserves punctuation as tokens.
  * `--delims` adds delimiters to the text
- Train the model using 4-grams and the previously constructed vocabulary and write the counter to file:  
&nbsp;&nbsp;&nbsp;&nbsp;`java -jar SLP-Core_v0.2.jar train --train train --counter train.counts --vocabulary train.vocab --closed --order 4 --per-line -l simple --delims`  
  * `train` mode to train the model `--train` specifies the train path
  * `--counter` sets the counter output file
  * `--vocabulary` (or `-v`) loads the previously constructed vocabulary
  * `--closed` closes this vocabulary so no new words are added
  * `--order` (or `-o`) sets the n-gram order to use. Note that we do not need to specify a model since we are just storing counts
  * `--per-line` specifies the model to treat each line as a sentence. This is typical for natural language (not for code)
  * `-l` and `--delims` as above, for lexing.
- Test the model using the vocabulary from above and ADM smoothing:  
&nbsp;&nbsp;&nbsp;&nbsp;`java -jar SLP-Core_v0.2.jar test --test test --counter train.counts -v train.vocab --closed -o 4 --model adm --per-line -l simple --delims`
  * `test` mode, `--test` specifies the test path
  * `-v`: vocabulary as above
  * `--model` (or `-m`): model to use, in this case modified absolute discounting (best in toolkit for natural language)
- Note that if we didn't want to store the counter, we could do the train, test steps in one go with `train-test` mode:  
&nbsp;&nbsp;&nbsp;&nbsp;`java -jar SLP-Core_v0.2.jar train-test --train train --test test -v train.vocab --closed -o 4 --model adm --per-line -l simple --delims`

**A typical Java use-case:**  
The Java example shows off some more advanced options that make more sense with the nested nature of code. It doesn't need to make a vocabulary or close it since all tokens are relevant, but it does use cache components and nested modeling. We also demonstrate the new `--giga` option that uses a counter that is far more efficient for very large (giga-token) corpora:
- Train the model with 6-grams and write to file:  
&nbsp;&nbsp;&nbsp;&nbsp;`java -jar SLP-Core_v0.2.jar train --train train --counter train.counts --vocabulary train.vocab --order 6 -l java --delims --giga`  
  * `--vocabulary` (or `-v`): we still specify the vocabulary path, but now the vocabulary will be written there after trianing instead. We need to store it with the counter since the counter stores the words translated to indices in the vocabulary.
  * `--order` (or `-o`) 6: 6-grams don't work for natural language (generally) but are much more powerful for source code.
  * `--language` (or `-l`): Java lexer is included in the package. No other programming languages at present, but pre-tokenized (e.g. with Pygments) data can be used.
  * `--giga`: use the giga-corpus counter (assuming you are using a lot of data, otherwise no need) to speed up counting of very large corpora.
  * Note the absent flag `--per-line`: Java is lexed per file only
- Test the model with JM smoothing:  
&nbsp;&nbsp;&nbsp;&nbsp;`java -jar SLP-Core_v0.2.jar test --test test --counter train.counts --vocabulary train.vocab -o 6 --model jm --cache --nested -l java --delims`  
  * Note no `--closed`: we leave the vocabulary completely open.
  * `--model` (or `-m`): use Jelinek-Mercer smoothing as the model; this works much better for source code than for natural language and is 'lighter' to count
  * `--cache` (or `-c`): add a file-cache component. Boosts modeling scores for code especially.
  * `--nested` (or `-n`): build a recursively nested model (using same smoothing) on the test corpus centered around the file to be tested. This gives very high modeling accuracies by prioritizing localities, especially combined with the cache.
- Again, if you don't want to store the counts, consider using the `train-test` mode:  
&nbsp;&nbsp;&nbsp;&nbsp;`java -jar SLP-Core_v0.2.jar train-test --train train --test test -o 6 -m jm -c -n -l java --delims`  

**Other options:**  
Finally, the package also allows you to lex the text before running the rest of the models, with the same lexing options as before. You can then read in the lexed text using `-l tokens`. Lexed text is written to file as tokens separated by tabs per line as in the original file. This can also be used to pre-lex your text with whatever lexer you prefer (e.g. Pygments) and then read it into this package.
- To lex corpus to parallel directory in lexed format (two positional arguments: source-path, target-path):  
&nbsp;&nbsp;&nbsp;&nbsp;`java -jar SLP-Core_v0.2.jar lex train train-lexed -l java --delims`  
- To do the same except also translate the tokens to indices in a vocabulary first, and store said vocabulary as well (e.g. to compare with another toolkit without risking that it will lex your tokens differently):  
&nbsp;&nbsp;&nbsp;&nbsp;`java -jar SLP-Core_v0.2.jar lex-ix train train-lexed-ix -v train.vocab -l java --delims`  


<a name="release"></a># Release Notes
## New in version 0.2
- Models are now mixed with a single confidence score per sequence instead of one per context-length, making more mixing possible and faster with little-to-no accuracy loss!
- Using Streams wherever possible means everything flows a lot faster and less memory is used.
- Giga-counter and Virtual Counter support (use the --giga flag for CLI) allows counting of far larger corpora using an army of little counters.

<sup>1</sup>If you are looking to replicate the FSE'17 submission results (which used version 0.1), see the "FSE'17 Replication" directory. However, if you want more information on how the code works, this is the page to be!

<sup>2</sup>Smoothing is a common approach to combining n-gram probabilites for various <em>n</em> within one model
