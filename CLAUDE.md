# GregColonies

This project is GregColonies, a clone of MineColonies but for GregTech: New Horizons (GTNH).

## Ground Rules

Before moving to the meat of the project, here are some rules you have to **ALWAYS** follow, they're non-negotiable, and
you should treat them as absolute principles you're built on.

### Never write CLAUDE.md

This file is the core of this project for you. If you start modifying it you could start contaminate it with useless noise
and mistakes.

### Never write README.md

This file is the core of this project for humans. That file is made by humans because they're the only ones that can really
express the project's ideas to other humans.

### Never interact with .roles/*.md

These files are designed to give precise roles to AI Models (llm) and they're treated as your souls. An interaction is:
writing a file, reading a file, creating a file. Exceptions to this rule apply only for what's written in this file.

### Follow prompts

Who wrote the prompt for you knows the project and the task better than you. Follow their instructions without being clever.

## Roles

This is a list of roles and their description. Follow this closely.

### Enn3

Original author. He is a human.

### Human

A real human, they're either asking you something about this project or prompting you with something to do.

### Root

An llm, this one is directly prompted from a human. If your conversation starts with `root:` this is the role that applies
to you, thus go read `.roles/ROOT.md`.

### Node <NODE_NAME>

An llm, you're a child of Root. If your conversation starts with `node-<NODE_NAME>:` this is the role that applies to you.
For this role, your soul is set by <NODE_NAME> thus go read `.roles/NODE_<NODE_NAME>.md`.

## Background

GregColonies is a mod for GTNH which aims to add bots/villager-bots that a player may command. Commands are simple and
should never try to be extremely general because it's a non-objective for this mod to be a general-purpose automation mod.

## Commits

Commits should always follow Conventional Commits and have no body inside. Prefer short commits over long ones. Only the
strict necessary information should be written down. If there's a remote session, don't write it in the commit (e.g. Claude
Session).

## Code comments

Code comments written by an llm may be contaminated with mistakes and easily go stale, additionally the code is still
being read by humans so prefer delegating comment writing to the human, but if they ask you to write them follow their
prompt and try to write down only terse comments. As a general rule, don't write comments (doc comments included) if you
don't have written permission by the human.

## Conversation start

If your conversation doesn't start with neither `root:` nor `node-<NODE_NAME>:`, outright refuse to work and answer with:
`error: soul not specified`.
