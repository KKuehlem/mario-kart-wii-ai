# Mario Kart Wii AI by Minekonst
Hey there, this is a self-learning AI for Mario Kart Wii, that I developed in my spare time in 2020 / 2021. I recently ported it to GitHub from my university GitLab, which is the reason why there is only one commit (I took the chance to get rid of some junk files).
The AI can learn Mario Kart Wii Tracks using mutations-based learning based on FOV-based input methods and is capable of e.g. frame-perfect game synchronization and learning on multiple hosts using a server-client system.

Although I have not worked on this project in some time, I made it public in case anybody might wanna use parts of it or continue the project :D

If you have any questions, feel free to write me a PN or write me on [Discord](discordapp.com/users/412221624302043158). Please note that this project might need some setting up (e..g for the Telegram Bot etc.).

# Features

## Fully functional
Here is a list of fully functional features:
- Learning tracks using mutation-based learning
- Server-Client System
- Performance optimization by not rendering the frames
- Frame-perfect synchronization between the client and the dolphin instance
    - The dolphin instance uses a Lua script, which communicates with the Java client using TCP sockets
- Track set-up, by
    - Recording the ground data through the client (needed for the current input methods)
    - Creating savestates for track loading
- Saving and loading the created AIs
- Replaying AIs and self-recorded driving
- Telegram-Bot which lets you view and modify ongoing training

## Half-way implemented
Here are some features, that I partly implemented, but due to limited spare time, never finished:
- Using other learning methods than mutation-based learning, e.g. DeepQLearning
- Using other input methods than the FOV-based (and the older minimap-like input method), such as using a CNN on the visual frames
- Packaging as docker containers and porting to Linux

## Planned but never implemented
Here are some features I never implemented but had on my mind, if you like some ideas on how the project could be continued:
- Cleaning up the project, e.g.
    - Removing some unused classes and fields
    - Using proper git submodules for [Dolphin Lua Core by SwareJonge](https://github.com/SwareJonge/Dolphin-Lua-Core) alongside updating it
    - Removing/porting the libraries in the `lib` folder to maven
- Replacing the server with a web interface

# Using my Code and Contribution
Feel free to use my code as you want or fork my project. If you like, I would be happy, if you credit my original work and consider opening a pull request :D

# Installation and Project Structure

## Project Structure
All the main code is in the `src` folder.
In the `Dolphin` folder is the current version of Dolphin which can be built with the code in the `Dolphin-Lua-Core-master` folder, which is a modified version of [Dolphin Lua Core by SwareJonge](https://github.com/SwareJonge/Dolphin-Lua-Core). The main features added are the ability to disable rendering for better performance and 
The Lua Script can be found at `Dolphin/Sys/Scripts/_AI.lua`.

## Libs
Use `BaseLib_Maven.cmd` to install the local jar as a maven package.

## Mario Kart Wii ISO
You need a Mario Kart Wii ISO named `MKWii.iso` and placed in the `Dolphin` folder. Most of the AI should work with any version but certain memory values are only for the NTSC version (_probably_ only the hard-coded values in the `_AI.lua` script). 

## Cuda
You need to install Cuda to run the application on a GPU. Needed Version is (exactly) [11.2.1](https://developer.nvidia.com/cuda-11.2.1-download-archive).
The cuda libraries need to be in the PATH or set via -Djava.library.path.
