# PixelForum
## 📝 Summary
🎮 This video game forum web application is designed for players 👾 who want to share their experiences, discuss their favorite titles, and connect with other gamers around the world 🌍.
Instead of a large generic forum where all kinds of unrelated topics are mixed ❌, this platform is focused exclusively on video games 🕹️, creating a dedicated space for players.
The application lets users create posts ✍️, comment on discussions 💬, give likes 👍, and customize their profile with an avatar 👤. In addition, it offers structured categories by game 🎯, image uploads 📷, popularity charts 📊, notifications 🔔, and advanced features such as, trending posts and real-time updates ⚡, providing a complete and organized platform for gamer interaction 🚀.

At this stage, only the functional and technical objectives of the application have been defined. The development and implementation of the system have not yet started.

## 🎯 Objectives
### ✅ Functional Objectives
The main functional objective of this application is to provide an online forum specialized in video games, where users can interact in an easy and dynamic way. The platform aims to facilitate the creation and participation in discussions, organize topics by game categories, and strengthen the community through social interaction tools and multimedia content.

Planned functionalities:
1. User registration and authentication with customizable profiles (including avatar).
2. Creation of posts related to video games.
3. Comment system on posts to encourage participation.
4. Possibility to give likes to posts and comments.
5. Organization of content through categories and specific games.
6. Image upload for profiles and posts.
7. Visualization of charts about game popularity or user activity.
8. Real-time email notifications.

### ⚙️ Technical Objectives
From a technical perspective, the application is conceived as a modern web development based on robust and scalable technologies. The goal is to implement a secure backend with API support, a dynamic frontend that ensures a good user experience, and complementary features such as chart visualization, file upload, and integration of external services.

Planned technical aspects:
1. Development with Spring Boot (Java) for business logic and REST APIs.
2. Development of the frontend with Angular to create a dynamic, modular, and maintainable user interface, ensuring a smooth interaction with the backend through REST APIs.
3. Relational database (MySQL) for managing users, posts, comments, and topics.
4. Image upload implementation.
5. Implementation of automated testing using JUnit for backend unit and integration tests, and Selenium for frontend end-to-end testing of the web interface.
6. Integration of notifications via email and/or websockets.
7. Security through authentication and authorization with JWT.
8. Static code analysis with cloud-based tools (SonarQube) to continuously monitor code quality and review reported issues throughout the development process.
9. Native packaging with GraalVM to generate a native image of the backend application, improving performance and reducing startup time.

## 🛠️ Methodology
The development of this project will follow a high-level iterative methodology, dividing the work into several clearly defined phases. Each phase has specific goals and deliverables, and the progress will be continuously monitored to ensure the quality and consistency of the implementation. This approach allows for incremental improvements, early detection of issues, and adaptation to feedback throughout the project.

### 📅 Phases
- Phase 1 – Definition of functionalities and screens:
In this phase, the general and detailed functionalities of the application will be defined. Additionally, initial wireframes and navigation flow designs will be created to guide the structure and user experience.

- Phase 2 – Repository, testing, and CI:
A Git repository will be created, along with the client and server projects. The minimum functionality required to connect the client, server, and database will be implemented. Initial automated tests will be developed, and the Continuous Integration (CI) system will be configured.

- Phase 3 – Version 0.1: Basic functionality and Docker:
The application will be extended to include all basic functionalities, together with their corresponding automated tests. The project will be containerized using Docker, and continuous delivery will be introduced. Version 0.1 of the application will then be released.

- Phase 4 – Version 0.2: Intermediate functionality:
The application will be expanded to include intermediate functionalities, together with their corresponding automated tests. The application will also be deployed in this phase, and version 0.2 will be released.

- Phase 5 – Version 1.0: Advanced functionality:
The application will be finalized with the advanced functionalities and released as version 1.0.

- Phase 6 – Writing the final report:
The memory/documentation of the project will be drafted, including objectives, design, implementation details, and conclusions.

- Phase 7 – Preparation of the presentation:
The final presentation for the defense will be prepared, including slides, demos, and supporting materials.

### ⏳ Timeline
|  Phase  | Start | End |
|---------|-------|-----|
| Phase 1 | 17/09/2025 | 27/09/2025 |
| Phase 2 |  |  |
| Phase 3 |  |  |
| Phase 4 |  |  |
| Phase 5 |  |  |
| Phase 6 |  |  |
| Phase 7 |  |  |

### 📊 Gantt Chart
A Gantt chart will be created to visually represent these phases, showing the schedule, dependencies, and milestones.

## 📋 Detailed Functionalities
The functionalities of the application are classified according to their complexity and the type of user they target. They are divided into basic, intermediate, and advanced functionalities to clearly define their scope and purpose. *‘Only owned’ means the user can only modify or delete their own information.

### 🟢 Basic Functionalities
|  Functionality  | Guest User | Registered User | Administartor |
|-----------------|------------|-----------------|---------------|
| Registration/Login | ![Yes](https://img.shields.io/badge/Yes-brightgreen) | ![Yes](https://img.shields.io/badge/Yes-brightgreen) | ![Yes](https://img.shields.io/badge/Yes-brightgreen) |
| View Post | ![Yes](https://img.shields.io/badge/Yes-brightgreen) | ![Yes](https://img.shields.io/badge/Yes-brightgreen) | ![Yes](https://img.shields.io/badge/Yes-brightgreen) |
| View public profile | ![Yes](https://img.shields.io/badge/Yes-brightgreen) | ![Yes](https://img.shields.io/badge/Yes-brightgreen) | ![Yes](https://img.shields.io/badge/Yes-brightgreen) |
| Create/Edit/Delete Post | ![No](https://img.shields.io/badge/No-red) | ![Yes](https://img.shields.io/badge/Yes-brightgreen) (Only owned) | ![Yes](https://img.shields.io/badge/Yes-brightgreen) |
| Create/Edit/Delete Comment on posts | ![No](https://img.shields.io/badge/No-red) | ![Yes](https://img.shields.io/badge/Yes-brightgreen) (Only owned) | ![Yes](https://img.shields.io/badge/Yes-brightgreen) |
| Give likes | ![No](https://img.shields.io/badge/No-red) | ![Yes](https://img.shields.io/badge/Yes-brightgreen) | ![Yes](https://img.shields.io/badge/Yes-brightgreen) |
| Add topic to post | ![No](https://img.shields.io/badge/No-red) | ![Yes](https://img.shields.io/badge/Yes-brightgreen) | ![Yes](https://img.shields.io/badge/Yes-brightgreen) |
| Upload image to profile and post | ![No](https://img.shields.io/badge/No-red) | ![Yes](https://img.shields.io/badge/Yes-brightgreen) | ![Yes](https://img.shields.io/badge/Yes-brightgreen) |

### 🟡 Intermediate Functionalities
|  Functionality  | Guest User | Registered User | Administartor |
|-----------------|------------|-----------------|---------------|
| View charts | ![Yes](https://img.shields.io/badge/Yes-brightgreen) | ![Yes](https://img.shields.io/badge/Yes-brightgreen) | ![Yes](https://img.shields.io/badge/Yes-brightgreen)|
| View trending posts | ![Yes](https://img.shields.io/badge/Yes-brightgreen) | ![Yes](https://img.shields.io/badge/Yes-brightgreen) | ![Yes](https://img.shields.io/badge/Yes-brightgreen) |
| Comment on other comments | ![No](https://img.shields.io/badge/No-red) | ![Yes](https://img.shields.io/badge/Yes-brightgreen) | ![Yes](https://img.shields.io/badge/Yes-brightgreen) |
| View private profile data | ![No](https://img.shields.io/badge/No-red) | ![Yes](https://img.shields.io/badge/Yes-brightgreen) (Only owned) | ![Yes](https://img.shields.io/badge/Yes-brightgreen) |
| Create/Edit/Remove topics | ![No](https://img.shields.io/badge/No-red) | ![No](https://img.shields.io/badge/No-red) | ![Yes](https://img.shields.io/badge/Yes-brightgreen) |
| Admin posts and comments | ![No](https://img.shields.io/badge/No-red) | ![No](https://img.shields.io/badge/No-red) | ![Yes](https://img.shields.io/badge/Yes-brightgreen) |

### 🔴 Advanced Functionalities
|  Functionality  | Guest User | Registered User | Administartor |
|-----------------|------------|-----------------|---------------|
| Notifications system | ![No](https://img.shields.io/badge/No-red) | ![Yes](https://img.shields.io/badge/Yes-brightgreen) (Only owned) | ![Yes](https://img.shields.io/badge/Yes-brightgreen) |
| Recommendation algorithm | ![No](https://img.shields.io/badge/No-red) | ![Yes](https://img.shields.io/badge/Yes-brightgreen) | ![Yes](https://img.shields.io/badge/Yes-brightgreen) |
| Password recovery | ![No](https://img.shields.io/badge/No-red) | ![Yes](https://img.shields.io/badge/Yes-brightgreen) | ![Yes](https://img.shields.io/badge/Yes-brightgreen) |
| Ban users | ![No](https://img.shields.io/badge/No-red) | ![No](https://img.shields.io/badge/No-red) | ![Yes](https://img.shields.io/badge/Yes-brightgreen) |

## 📑 Analysis
### 🖥️ Screens and navigation:
The images below present a preliminary mockup of the web application, showcasing its overall design and visual style. From any page, users will have seamless access to all the platform’s main functionalities, ensuring intuitive and consistent navigation.

#### Main feed
![Mockup](Images/Feed.png)

#### Create post screen
![Mockup](Images/CreatePost.png)

#### MY profile screen
![Mockup](Images/Profile.png)

#### Charts screen
![Mockup](Images/Charts.png)

### 🧩 Entities:
| Entity | Attributes | Description |
| ------ | ---------- | ----------- |
| User | `id`, `username`, `email`, `passwordHash`, `avatar`, `role`, `likePost`, `likeComment` | Represents a platform user. Can be a registered user or administrator. Stores authentication data, profile information, and permissions. |
| Post | `id`, `title`, `content`, `createdAt`, `updatedAt`, `images`, `userId`, `topicsId`, `likes`, `usersThatLike` | Represents a discussion thread or contribution created by a registered user. It may include text, images, and likes. Linked to a topic and an author. |
| Comment | `id`, `content`, `createdAt`, `updatedAt`, `userId`, `postId`, `commentId`, `likes`, `usersThatLike` | Represents a reply to a post. Written by registered users, it can also receive likes and is always associated with both a post and an author and sometimes to other comment. |
| Topic | `id`, `name`, `description` | Groups posts under a common topic (Action Games, RPG, Indie or specific games). Facilitates navigation and content organization. |

*Note: Additional attributes may be added during the development process if new requirements arise or to enhance the application’s functionality.

The following diagram shows the relationships between the main entities of the application.

![Inicio](Images/Entidades.png)

### 🔑 User permissions:
- Guest user: Can view all public information on the platform, including posts, comments, charts, and user profiles.
- Registered user: In addition to guest permissions, can create posts, write comments, edit and delete their own posts and comments, upload a profile picture, recover their password, check their private information, and receive notifications and suggestions based on the recommendation algorithm.
- Administrator: Has full permissions, including those of registered users, plus the ability to edit or delete any post/comment, manage topics, and moderate user accounts.

### 🖼️ Images:
The User entity will support a profile picture. The Post entity may optionally include one or more images attached to the content.

### 📈 Charts:
A statistics section will be included to visualize community activity. For example, a bar chart will show the number of posts created per topic in the last month, and a line chart will display the evolution of total posts over time.

### 💡 Complementary technology:
The project will integrate email sending for password recovery and account-related notifications. Additionally, WebSockets will be used to enable real-time notifications, ensuring that users are immediately informed about interactions such as new comments, likes, or administrative updates.

### 🧠 Advanced algorithm or query:
An advanced recommendation algorithm will be implemented to suggest relevant posts or categories to each user based on their past activity.

## 📌 Tracking / Progress
In order to keep track of the project: a blog will be maintained with announcements and updates about the development and a GitHub Project board will be used to manage tasks, track progress, and organize development activities.

Project blog: https://medium.com/@m.gutierrezp.2022

GitHub Project: https://github.com/orgs/codeurjc-students/projects/35

## 👤 Author
This application is developed for my Degree's Final Project for the Computer Science degree at the ETSII of URJC.

Author and Supervisor:
| Role | Name | Mail | GitHub |
|------|------|------|--------|
| Student | Martín Gutiérrez Parada | m.gutierrezp.2022@alumnos.urjc.com | [GitHub](https://github.com/martingutpar) |
| Tutor | Michel Maes Bermejo | michel.maes@urjc.es | [GitHub](https://github.com/Maes95) |
