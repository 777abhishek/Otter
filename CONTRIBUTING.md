# Contributing to Otter

Thank you for your interest in contributing to Otter! This document provides guidelines and instructions for contributing to the project.

## Code of Conduct

- Be respectful and inclusive
- Provide constructive feedback
- Focus on what is best for the community
- Show empathy towards other contributors

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 36 (Android 16)
- Git

### Setup Development Environment

1. Fork the repository
2. Clone your fork:
```bash
git clone https://github.com/YOUR_USERNAME/Otter.git
cd Otter
```

3. Add upstream remote:
```bash
git remote add upstream https://github.com/777abhishek/Otter.git
```

4. Create a feature branch:
```bash
git checkout -b feature/your-feature-name
```

5. Open in Android Studio and let Gradle sync complete

## Development Workflow

### 1. Code Style

We use Detekt and KtLint to maintain code quality:

```bash
# Check code style
./gradlew ktlintCheck detekt

# Auto-fix issues
./gradlew ktlintFormat
```

### 2. Run Tests

Before committing, run all tests:

```bash
# Unit tests
./gradlew test

# Instrumentation tests
./gradlew connectedAndroidTest
```

### 3. Build Project

Ensure the project builds successfully:

```bash
./gradlew assembleDebug
```

## Making Changes

### Branch Naming

Use descriptive branch names:
- `feature/add-video-download-queue`
- `fix/player-crash-on-rotate`
- `docs/update-readme`
- `refactor/clean-up-repository`

### Commit Messages

Follow conventional commits:

```
<type>(<scope>): <subject>

<body>

<footer>
```

Types:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

Examples:
```
feat(player): add background playback support

Implement audio-only playback when screen is off.
Uses ExoPlayer's audio focus management.

Closes #123
```

### Pull Request Guidelines

1. **Update documentation** if needed
2. **Add tests** for new functionality
3. **Ensure all tests pass**
4. **Update CHANGELOG.md** for user-facing changes
5. **Link related issues** in PR description

PR Template:
```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
Describe tests added/updated

## Checklist
- [ ] Code follows project style
- [ ] Tests pass locally
- [ ] Documentation updated
- [ ] No merge conflicts
```

## Code Review Process

1. Automated checks run on every PR
2. Maintainers review code
3. Address review comments
4. Update PR when changes are ready
5. Approval required for merge

## Project Structure

```
app/src/main/kotlin/com/Otter/app/
├── data/              # Data layer
│   ├── auth/         # Authentication
│   ├── database/     # Room database
│   ├── download/     # Download management
│   ├── models/       # Data models
│   ├── repositories/ # Data repositories
│   └── ytdlp/        # yt-dlp integration
├── di/               # Dependency injection
├── network/          # API services
├── player/           # Video player
├── service/          # Background services
├── ui/               # UI layer
│   ├── components/   # Reusable composables
│   ├── download/     # Download screens
│   ├── navigation/   # Navigation setup
│   ├── screens/      # Screen composables
│   ├── theme/        # Material3 theme
│   └── viewmodels/   # ViewModels
├── util/             # Utilities
└── work/             # WorkManager workers
```

## Adding New Features

1. Create feature branch
2. Implement feature following architecture
3. Add tests (unit + instrumentation)
4. Update documentation
5. Submit PR

### Example: Adding a New Screen

1. **Create ViewModel**:
```kotlin
@HiltViewModel
class NewFeatureViewModel @Inject constructor(
    private val repository: FeatureRepository
) : ViewModel() {
    // Implementation
}
```

2. **Create Screen Composable**:
```kotlin
@Composable
fun NewFeatureScreen(
    viewModel: NewFeatureViewModel = hiltViewModel()
) {
    // UI implementation
}
```

3. **Add Navigation**:
```kotlin
fun NavGraphBuilder.newFeatureScreen() {
    composable("new_feature") {
        NewFeatureScreen()
    }
}
```

4. **Add Tests**:
- Unit tests for ViewModel
- Compose tests for UI

## Reporting Bugs

Before reporting bugs:

1. Check existing issues
2. Verify it's not a duplicate
3. Provide minimal reproduction
4. Include device info and logs

Bug Report Template:
```markdown
## Description
Clear description of bug

## Steps to Reproduce
1. Step 1
2. Step 2
3. Step 3

## Expected Behavior
What should happen

## Actual Behavior
What actually happens

## Environment
- Android version:
- App version:
- Device:

## Logs
Relevant log output
```

## Feature Requests

1. Check existing requests
2. Use clear, descriptive title
3. Explain use case
4. Consider implementation complexity

## Questions?

- Open an issue with the `question` label
- Join discussions in existing issues
- Check documentation first

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
