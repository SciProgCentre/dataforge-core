[![JetBrains Research](https://jb.gg/badges/research.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

[ ![Download](https://api.bintray.com/packages/mipt-npm/dataforge/dataforge-meta/images/download.svg) ](https://bintray.com/mipt-npm/dataforge/dataforge-meta/_latestVersion)

[![DOI](https://zenodo.org/badge/148831678.svg)](https://zenodo.org/badge/latestdoi/148831678)

# Questions and Answers #

In this section we will try to cover DataForge main ideas in the form of questions and answers.

## General ##

**Q:** I have a lot of data to analyze. The analysis process is complicated, requires a lot of stages and data flow is not always obvious. To top it the data size is huge, so I don't want to perform operation I don't need (calculate something I won't need or calculate something twice). And yes, I need it to be performed in parallel and probably on remote computer. By the way, I am sick and tired of scripts that modify other scripts that control scripts. Could you help me?

**A:** Yes, that is the precisely the problem DataForge was made to solve. It allows to perform some automated data manipulations with automatic optimization and parallelization. The important thing that data processing recipes are made in the declarative way, so it is quite easy to perform computations on a remote station. Also DataForge guarantees reproducibility of analysis results.
<hr>

**Q:** How does it work?

**A:** At the core of DataForge lies the idea of **metadata processor**. It utilizes the statement that in order to analyze something you need data itself and some additional information about what does that data represent and what does user want as a result. This additional information is called metadata and could be organized in a regular structure (a tree of values not unlike XML or JSON). The important thing is that this distinction leaves no place for user instructions (or scripts). Indeed, the idea of DataForge logic is that one do not need imperative commands. The framework configures itself according to input meta-data and decides what operations should be performed in the most efficient way.
<hr>

**Q:** But where does it take algorithms to use?

**A:** Of course algorithms must be written somewhere. No magic here. The logic is written in specialized modules. Some modules are provided out of the box at the system core, some need to be developed for specific problem.
<hr>

**Q:** So I still need to write the code? What is the difference then?

**A:** Yes, someone still need to write the code. But not necessary you. Simple operations could be performed using provided core logic. Also your group can have one programmer writing the logic and all other using it without any real programming expertise. Also the framework organized in a such way that one writes some additional logic, he do not need to thing about complicated thing like parallel computing, resource handling, logging, caching etc. Most of the things are done by the DataForge.
<hr>

## Platform ##

**Q:** Which platform does DataForge use? Which operation system is it working on?

**A:** The DataForge is mostly written in Java and utilizes JVM as a platform. It works on any system that supports JVM (meaning almost any modern system excluding some mobile platforms).
 <hr>

 **Q:** But Java... it is slow!

 **A:** [It is not](https://stackoverflow.com/questions/2163411/is-java-really-slow/2163570#2163570). It lacks some hardware specific optimizations and requires some additional time to start (due to JIT nature), but otherwise it is at least as fast as other languages traditionally used in science. More importantly, the memory safety, tooling support and vast ecosystem makes it №1 candidate for data analysis framework.

<hr>

 **Q:** Can I use my C++/Fortran/Python code in DataForge?

 **A:** Yes, as long as the code could be called from Java. Most of common languages have a bridge for Java access.  There are completely no problems with compiled C/Fortran libraries. Python code could be called via one of existing python-java interfaces. It is also planned to implement remote method invocation for common languages, so your Python, or, say, Julia, code could run in its native environment. The metadata processor paradigm makes it much easier to do so.

<hr>

## Features ##

**Q:** What other features does DataForge provide?

**A:** Alongside metadata processing (and a lot of tools for metadata manipulation and layering), DataForge has two additional important concepts:

* **Modularisation**. Contrary to lot other frameworks, DataForge is intrinsically modular. The mandatory part is a rather tiny core module. Everything else could be customized.

* **Context encapsulation**. Every DataForge task is executed in some context. The context isolates environment for the task and also works as dependency injection base and specifies interaction of the task with the external world.


<hr>

**Q:** OK, but now I want to work directly with my measuring devices. How can I do that?

**A:** The [dataforge-control](${site.url}/docs.html#control) module provides interfaces to interact with the hardware. Out of the box it supports safe communication with TCP/IP or COM/tty based devices. Specific device declaration could be done via additional modules. It is also possible to maintain data storage with [datforge-storage](${site.url}/docs.htm#storage) module.

<hr>

**Q:** Declarations and metadata are good, but I want my scripts back!

**A:** We can do that. [GRIND](${site.url}/docs.html#grind) provides a shell-like environment called GrindShell. It allows to run imperative scripts with full access to all of the DataForge functionality. Grind scripts are basically context-encapsulated. Also there are convenient feature wrappers called helpers that could be loaded into the shell when new features modules are added.

<hr>

## Misc ##

**Q:** So everything looks great, can I replace my ROOT / other data analysis framework with DataForge?

**A:** One must note, that DataForge is made for analysis, not for visualisation. The visualisation and user interaction capabilities of DataForge are rather limited compared to frameworks like ROOT, JAS3 or DataMelt. The idea is to provide reliable API and core functionality. In fact JAS3 and DataMelt could be used as a frontend for DataForge mechanics. It is planned to add an interface to ROOT via JFreeHep AIDA.

<hr>

**Q:** How does DataForge compare to cluster computation frameworks like Hadoop or Spark?

**A:** Again, it is not the purpose of DataForge to replace cluster software. DataForge has some internal parallelism mechanics and implementations, but they are most certainly worse then specially developed programs. Still, DataForge is not fixed on one single implementation. Your favourite parallel processing tool could be still used as a back-end for the DataForge. With full benefit of configuration tools, integrations and no performance overhead.

<hr>

**Q:** Is it possible to use DataForge in notebook mode?

**A:** Yes, it is. DataForge can be used as is from [beaker/beakerx](http://beakernotebook.com/) groovy kernel with minor additional adjustments. It is planned to provide separate DataForge kernel to `beakerx` which will automatically call a specific GRIND shell.

<hr>

**Q:** Can I use DataForge on a mobile platform?

**A:** DataForge is modular. Core and the most of api are pretty compact, so it could be used in Android applications. Some modules are designed for PC and could not be used on other platforms. IPhone does not support Java and therefore could use only client-side DataForge applications.
