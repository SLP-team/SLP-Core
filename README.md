# Software is Language Too!
We present the Software Language Processing (SLP) library + CLI, here to enable fluent modeling of evolving text!

## Why?
Code is a kind of language, and language models are awesomely useful (think of Siri, auto-correct, chatbots; all powered by language models). In code, we are beginning to discover the benefits too: good language models enable great code completion, could actually help hunt down bugs, suggest better variable names and even help you get your pull requests accepted!

But code is also very different from natural languages like English in some crucial ways: it is dynamic, rapidly evolving with every commit, adding new identifiers, methods, modules and even entire new libraries. It is also deeply hierarchical, with files in packages in modules in eco-systems, etc. Traditional language models are too static to deal with this, so we present SLP: a dynamic, scalable language modeling library.
We don't just enable fluent language model mixing and updates, we boost modeling accuracy by up to a factor 4, outperforming some LSTMs, and getting even better when mixing with them!

# FAQ: How does it work?
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

# Getting Started
This core module contains everything needed to get started modeling language, including code to lex, build vocabularies, construct (complex) models and run those models. Other modules relying on Core for more exotic applications are forthcoming.

The code ships as a Java library and comes with a JAR (use v0.2) that serves as a command line API. To get started with the command line API, simply download the Jar and type `java -jar SLP-Core_v0.2.jar --help` to get your code modeling adventures started! To get started with the code as a library, either add the Jar to your dependencies or download the whole project and link it to yours. Have a look at slp.core.example to see how you would do all the setup for a natural language and a Java code example; it show-cases quite a few options that you can set.

# New in version 0.2
- Models are now mixed with a single confidence score per sequence instead of one per context-length, making more mixing possible and faster with little-to-no accuracy loss!
- Using Streams wherever possible means everything flows a lot faster and less memory is used.
- Giga-counter and Virtual Counter support (use the --giga flag for CLI) allows counting of far larger corpora using an army of little counters.

<sup>1</sup>If you are looking to replicate the FSE'17 submission results (which used version 0.1), see the "FSE'17 Replication" directory. However, if you want more information on how the code works, this is the page to be!

<sup>2</sup>Smoothing is a common approach to combining n-gram probabilites for various <em>n</em> within one model
