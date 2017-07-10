# Software is Language Too!
We present the Software Language Processing (SLP) library + CLI, here to enable fluent modeling of evolving text! Check out our [wiki](https://github.com/SLP-team/SLP_Core/wiki) for why we built this, how it works and a detailed getting started guide. Also keep an eye out on sibling projects in the SLP-team ecosystem!
Note: this tool was created as part of a publication at FSE'2017<sup>1</sup> and has since evolved to version 0.2

# Getting Started
This core module contains everything needed to get started with modeling language, including code to lex files, build vocabularies, construct (complex) models and run those models. Get the latest Jar and use it either as a [command-line tool](#cli) or add it as a dependency to your project ([code API](#codeapi)) to get started. Other modules relying on Core for more exotic applications are forthcoming.

## Code API
<a name="codeapi"></a>
The code is best used as a Java library, and well-documented examples of how to use this code for both NL and SE use-cases are in src/slp/core/example of this repo. To get started with the code as a library, either add the Jar to your dependencies (Maven dependency coming soon!) or download the whole project and link it to yours. See the wiki for more thorough explanation and don't hesitate to ask questions as an Issue!

## Command Line API
<a name="cli"></a>

**A very quick start:** running a 6-gram, JM, cache model on train and test Java directories:  
`java -jar SLP-Core_v0.2.jar train-test --train train-path --test test-path -m jm -o 6 -l java --cache`  

A break-down of what happened:
- `train-test`: the 'mode' in which it ran: train and test in one go. You can also train first, store the model (and vocabulary) and then load it later using the separate `train` and `test` modes. Keep in mind that a saved model is useless without its vocabulary! You can either build it separately (using `vocabulary` mode), or let `train` mode save it for you after training
- `--train`/`--test`: the paths (files, directories) to recursively train on
- `-m` (or `--model`): the n-gram model type to use. Use `adm` for a better natural language model, `jm` is standard (and fastest) for code
- `-o` (or `--order`): the maximum length of sequences to learn. The model complexity increases sharply with this number, and is normally no more than 5, but our tool can handle values up to about 10 on corpora of several millions of tokens, provided your RAM can too. 6 is a good pick for Java code in our experience; performance might decrease for higher values.
- `-l` (or `--language`): the language to use for the lexer (first step in any task). Currently we support java for code, and some simple lexers that split on e.g. punctuation and whitespace. See below for using your own.
- `--cache` (or `-c`): add a file-level cache of the same kind as the general model. See also: `--dynamic` (`-d`) and `--nested` (`-n`).

### More options
For other options, type `java -jar SLP-Core_v0.2.jar --help`. See our [wiki](https://github.com/SLP-team/SLP-Core/wiki/Usage:-command-line-API) for some examples of what sequence of commands might be applicable for a typical NLP and SLP use-case with more details. A quick overview of what else can be done:
- The `-e` (or `--extension`) flag lets you specify the regex extension (e.g.: 'java', 'c(pp)?') to match files against in your train/test directories. Only files matching this extension will be modeled
- `--verbose *file*` writes all tokens with their entropies for each file to an output file, with tab-separated tokens, each prefixed by their entropy followed by a special separator (ascii 31)
- There is also a (`train-`)`predict` mode, which asks each model for top 10 completions at every modeling point instead of entropy; every model sub-class must support predict API calls. Of course, batch-prediction doesn't really make sense for practical purposes, but in the future (and in your code) this API can be used for code completion tasks. Batch prediciton evaluation is a slower process, but all the parts are built-in to the tool. The resulting scores are then MRR scores, which reflect prediction accuracy (closer to 1 is better).

<a name="release"></a>
# Release Notes
## New in version 0.2
- Models are now mixed with a single confidence score per sequence instead of one per context-length, making more mixing possible and faster with little-to-no accuracy loss!
- Using Streams wherever possible means everything flows a lot faster and less memory is used.
- Giga-counter and Virtual Counter support (use the --giga flag for CLI) allows counting of far larger corpora using an army of little counters.

<sup>1</sup>If you are looking to replicate the FSE'17 results (which used version 0.1), see the "FSE'17 Replication" directory. However, if you want to work with a more advanced version of the code, this is the page to be! Our code-base is ever becoming faster and (hopefully) more accessible without compromising modeling accuracy.
