# The Latest
The toolkit is now in [v0.3](https://github.com/SLP-team/SLP-Core/releases/tag/v0.3). The interfaces have been redesigned to be more modular and intuitive with input from the community, special thanks to @dgopstein and @shaikhismail! We welcome all input to make this tool more useful for you.

# Software is Language Too!
We present the Software Language Processing (SLP) library + CLI, here to enable fluent modeling of evolving text! Check out our [wiki](https://github.com/SLP-team/SLP_Core/wiki) for why we built this, how it works and a detailed getting started guide. Also keep an eye out on upcoming sibling projects in the SLP-team ecosystem!  
_Note:_ this tool was created as part of a publication at FSE'2017<sup>1</sup> and has since evolved to version 0.3

# Quick Start
This core module contains everything needed to get started with modeling language, including code to lex files, build vocabularies, construct (complex) models and run those models. Get the [latest Jar](https://github.com/SLP-team/SLP-Core/releases) and use it either as a [command-line tool](#cli) or add it as a dependency to your project ([code API](#codeapi)) to get started. Other modules relying on SLP-Core for more exotic applications are forthcoming. Finally: any questions/ideas? Create an Issue! Any improvements/bug fixes? Submit a Pull Request! We greatly appreciate your help in making this useful for others.

## Code API
<a name="codeapi"></a>
The code is best used as a Java library, and well-documented examples of how to use this code for both NL and SE use-cases are in the `src/slp/core/example` package of this repo. Use this option if you want to go beyond summary metrics and build more interesting models and scenarios. To get started with the code as a library, either add [the latest Jar](https://github.com/SLP-team/SLP-Core/releases) to your dependencies (Maven dependency coming soon!) or download the whole project and link it to yours. See the [wiki](https://github.com/SLP-team/SLP_Core/wiki) for a more thorough explanation and don't hesitate to ask questions as an Issue!

## Command Line API
<a name="cli"></a>

**A very quick start:** running a 6-gram, JM, cache model on train and test Java directories:  
`java -jar SLP-Core_v0.2.jar train-test --train train-path --test test-path -m jm -o 6 -l java -e java --cache`  

A break-down of what happened:
- `train-test`: the 'mode' in which it runs: train and test in one go.
- `--train`/`--test`: the paths (files, directories) to recursively train/test on
- `-m` (or `--model`): the n-gram model type to use. Use `adm` for a better natural language model, `jm` is standard (and fastest) for code
- `-o` (or `--order`): the maximum length of sequences to learn. The model complexity increases sharply with this number, and is normally no more than 5, but our tool can handle values up to about 10 on corpora of several millions of tokens, provided your RAM can too. 6 is a good pick for Java code in our experience; performance might decrease for higher values.
- `-l` (or `--language`): the language to use for the lexer. Currently we support java for code, and some simple lexers that split on e.g. punctuation and whitespace. See below for using your own.
- `-e` (or `--extension`): the extension (regex) to use for filter files in the train/test path; other (here: non-java) files are ignored. The code API also allows regexes to match against the whole filename.
- `--cache` (or `-c`): add a file-level cache of the same type (JM-6) as the general model. See also: `--dynamic` (`-d`) and `--nested` (`-n`).

### More options
For other options, type `java -jar SLP-Core_v0.2.jar --help`. See our [wiki](https://github.com/SLP-team/SLP-Core/wiki/Usage:-command-line-API) for some examples of what sequence of commands might be applicable for a typical NLP and SLP use-case with more details. A quick overview of what else can be done:
- **Build your vocabulary first**, with the `vocabulary` mode. Write it to file and make sure to load it with the `--vocabulary *file*` flag when training/testing.
- **Build your model first** with the separate `train` and `test` modes for faster re-use. You can load it back in using the `--counter *file*` flag when testing. Keep in mind that a saved model is useless without its vocabulary! You can either build it separately as above, or let `train` mode save it for you when it saves its own model, by setting the `--vocabulary` flag to where you want it saved when running `train`.
- `--verbose *file*` **writes tokens with their entropies** for each file to an output file, with tab-separated tokens, each prefixed by their entropy followed by a special separator (ascii 31)
- There is also a (`train-`)`predict` mode, which asks each model for **top 10 completions** at every modeling point instead of entropy<sup>2</sup>
- Set the `--giga` flag for **counting huge corpora** quickly (new since v0.2!)

<a name="release"></a>
# Release Notes
## New in version 0.3
- The `LexerRunner`, `ModelRunenr` and `Vocabulary` are no longer static objects, but can be individually configured and used. This makes it possible to do things like work with non-overlapping vocabularies at the same time (and thus improves parallelism), use multiple differently configured models, and makes configuration more explicit overall.
- All the interfaces have been redesigned with community input to be more minimal and intuitive

<sup>1</sup>If you are looking to replicate the FSE'17 results (which used version 0.1), see the "FSE'17 Replication" directory. However, if you want to work with a more advanced version of the code, this is the page to be! Our code-base is ever becoming faster and (hopefully) more accessible without compromising modeling accuracy.

<sup>2</sup>Every Model supports predict API calls, though prediction is a bit slower than entropy evaluation. Of course, batch-prediction itself doesn't really make sense for practical purposes but is a useful evaluation metric, and the code API can be used for code completion tasks. The scores from this command are MRR scores, which reflect prediction accuracy (closer to 1 is better).
