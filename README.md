# The UnderFrogs - Team Project

## Team Members
- Marco Nocerino
- David Jayakumar
- Shane Ginty
- Jamie Montgomery

## Branching Strategy (GitFlow)
# Purpose
Our project follows a GitFlow-style branching model to keep development organized, support parallel feature work, and maintain stability.

## Core Branches
**main**
Production-ready code only. Every commit here should be deployable.
**develop**
Integration branch for completed features. This is the default branch for ongoing development.
**Supporting Branches**
feature/<short-description>
Created from develop for new features and improvements

## Workflow
1) **Start Feature Work**
Create a new feature branch from develop.
Keep commits focused and descriptive.
Rebase or merge develop regularly to stay up to date.

3) **Open Pull Request**
Open a pull request from feature/<short-description> into develop.

5) **Code Review Requirements**
At least 1 approved review is required before merge.
No direct pushes to main.
Resolve all review comments before merging.

4) **Merge Strategy**
Use squash merge for feature branches to keep history clean.
Delete feature branch after merge
