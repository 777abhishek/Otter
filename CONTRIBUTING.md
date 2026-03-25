# Contributing to Otter

Thank you for your interest in contributing to Otter! This document provides guidelines and instructions for contributing to the project.

## Code of Conduct

This project adheres to a [Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.

## Getting Started

### Prerequisites

- Android Studio (latest stable or Canary version)
- JDK 17 or higher
- Android SDK with API level 34+
- Git

### Building from Source

1. Fork the repository
2. Clone your fork:
   ```bash
   git clone https://github.com/YOUR_USERNAME/Otter.git
   cd Otter
   ```
3. Open the project in Android Studio
4. Wait for Gradle sync to complete
5. Build the project:
   ```bash
   ./gradlew assembleDebug
   ```

## Development Guidelines

### Code Style

- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add comments for complex logic
- Keep functions focused and concise

### Commit Messages

Use clear and descriptive commit messages:

- `feat: add new feature`
- `fix: resolve bug in video playback`
- `docs: update README`
- `style: format code`
- `refactor: restructure code`
- `test: add unit tests`
- `chore: update dependencies`

### Pull Request Process

1. Create a new branch from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   ```
2. Make your changes
3. Test your changes thoroughly
4. Commit your changes with clear messages
5. Push to your fork:
   ```bash
   git push origin feature/your-feature-name
   ```
6. Open a pull request to the main repository
7. Fill in the pull request template
8. Wait for review and address any feedback

### Before Submitting

- Ensure your code compiles without errors
- Run tests and ensure they pass
- Update documentation if needed
- Check for any formatting issues
- Verify your changes don't break existing functionality

## Reporting Issues

### Bug Reports

When reporting a bug, please include:

- **Title**: Clear and concise description
- **Description**: Detailed explanation of the issue
- **Steps to reproduce**: Step-by-step instructions
- **Expected behavior**: What should happen
- **Actual behavior**: What actually happens
- **Screenshots**: If applicable
- **Device info**: Device model, Android version
- **App version**: Version of Otter you're using
- **Logs**: Relevant logcat output if available

### Feature Requests

When requesting a feature:

- **Title**: Clear and concise description
- **Description**: Detailed explanation of the feature
- **Use case**: Why this feature is needed
- **Alternatives**: Any alternative solutions you've considered
- **Additional context**: Any other relevant information

## Translation

Otter supports multiple languages. You can help translate the app:

1. Visit our translation platform (to be set up)
2. Sign up for an account
3. Choose your language
4. Start translating

## Questions

For questions or discussions:

- Check existing issues and discussions
- Join our community (Telegram/Discord/Matrix - to be set up)
- Open a new discussion for general questions

## License

By contributing to Otter, you agree that your contributions will be licensed under the [GPL-3.0 License](LICENSE).

## Acknowledgments

Thank you to all contributors who help make Otter better!
