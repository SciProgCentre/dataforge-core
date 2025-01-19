[![JetBrains Research](https://jb.gg/badges/research.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![DOI](https://zenodo.org/badge/148831678.svg)](https://zenodo.org/badge/latestdoi/148831678)

## Publications

* [A general overview](https://doi.org/10.1051/epjconf/201817705003)
* [An application in "Troitsk nu-mass" experiment](https://doi.org/10.1088/1742-6596/1525/1/012024)

## Video

* [A presentation on application of DataForge (legacy version) to Troitsk nu-mass analysis.](https://youtu.be/OpWzLXUZnLI?si=3qn7EMruOHMJX3Bc)

## Questions and Answers

In this section, we will try to cover DataForge main ideas in the form of questions and answers.

### General

**Q**: I have a lot of data to analyze. The analysis process is complicated, requires a lot of stages, and data flow is not always obvious. Also, the data size is huge, so I don't want to perform operation I don't need (calculate something I won't need or calculate something twice). I need it to be performed in parallel and probably on remote computer. By the way, I am sick and tired of scripts that modify other scripts that control scripts. Could you help me?

**A**: Yes, that is precisely the problem DataForge was made to solve. It allows performing some automated data manipulations with optimization and parallelization. The important thing that data processing recipes are made in the declarative way, so it is quite easy to perform computations on a remote station. Also, DataForge guarantees reproducibility of analysis results.

**Q**: How does it work?

**A**: At the core of DataForge lies the idea of metadata processor. It utilizes the fact that to analyze something you need data itself and some additional information about what does that data represent and what does user want as a result. This additional information is called metadata and could be organized in a regular structure (a tree of values similar to XML or JSON). The important thing is that this distinction leaves no place for user instructions (or scripts). Indeed, the idea of DataForge logic is that one does not need imperative commands. The framework configures itself according to input meta-data and decides what operations should be performed in the most efficient way.

**Q**: But where does it take algorithms to use?

**A**: Of course algorithms must be written somewhere. No magic here. The logic is written in specialized modules. Some modules are provided out of the box at the system core, some need to be developed for a specific problem.

**Q**: So I still need to write the code? What is the difference then?

**A**: Yes, someone still needs to write the code. But not necessary you. Simple operations could be performed using provided core logic. Also, your group can have one programmer writing the logic and all other using it without any real programming expertise. The framework organized in a such way that one writes some additional logic, they do not need to think about complicated thing like parallel computing, resource handling, logging, caching, etc. Most of the things are done by the DataForge.

### Platform

**Q**: Which platform does DataForge use? Which operating system is it working on?

**A**: The DataForge is mostly written in Kotlin-multiplatform and could be used on JVM, JS and native targets. Some modules and functions are supported only on JVM

**Q**: Can I use my C++/Fortran/Python code in DataForge?

**A**: Yes, as long as the code could be called from Java. Most common languages have a bridge for Java access. There are completely no problems with compiled C/Fortran libraries. Python code could be called via one of existing python-java interfaces. It is also planned to implement remote method invocation for common languages, so your Python, or, say, Julia, code could run in its native environment. The metadata processor paradigm makes it much easier to do so.

### Features

**Q**: What other features does DataForge provide?

**A**: Alongside metadata processing (and a lot of tools for metadata manipulation and layering), DataForge has two additional important concepts:

* **Modularisation**. Contrary to lot other frameworks, DataForge is intrinsically modular. The mandatory part is a rather tiny core module. Everything else could be customized.

* **Context encapsulation**. Every DataForge task is executed in some context. The context isolates environment for the task and also works as dependency injection base and specifies interaction of the task with the external world.

### Misc

**Q**: So everything looks great, can I replace my ROOT / other data analysis framework with DataForge?

**A**: One must note that DataForge is made for analysis, not for visualization. The visualization and user interaction capabilities of DataForge are rather limited compared to frameworks like ROOT, JAS3 or DataMelt. The idea is to provide reliable API and core functionality. [VisionForge](https://git.sciprog.center/kscience/visionforge) project aims to provide tools for both 2D and 3D visualization both locally and remotely.

**Q**: How does DataForge compare to cluster computation frameworks like Apache Spark?

**A**: It is not the purpose of DataForge to replace cluster computing software. DataForge has some internal parallelism mechanics and implementations, but they are most certainly worse than specially developed programs. Still, DataForge is not fixed on one single implementation. Your favourite parallel processing tool could be still used as a back-end for the DataForge. With full benefit of configuration tools, integrations and no performance overhead.

**Q**: Is it possible to use DataForge in notebook mode?

**A**: [Kotlin jupyter](https://github.com/Kotlin/kotlin-jupyter) allows using any JVM program in a notebook mode. The dedicated module for DataForge is work in progress.

${modules}
