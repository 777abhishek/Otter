# Otter Architecture Documentation

## Overview

Otter follows a clean architecture pattern with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────┐
│                      UI Layer                             │
│  (Screens, Composables, ViewModels)                     │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                    Domain Layer                          │
│  (Use Cases, Business Logic)                            │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                    Data Layer                            │
│  (Repositories, Data Sources, Models)                   │
└─────────────────────────────────────────────────────────┘
```

## Layer Responsibilities

### UI Layer (`ui/`)

**Responsibilities:**
- Display data and handle user interactions
- Observe state changes from ViewModels
- Navigate between screens
- No business logic

**Components:**
- **Screens**: Full-screen composables (e.g., `HomeScreen`, `PlayerScreen`)
- **Components**: Reusable UI elements (e.g., `VideoCard`, `PlayerControls`)
- **ViewModels**: Manage UI state and handle user events

**Example:**
```kotlin
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    when (uiState) {
        is HomeUiState.Loading -> LoadingView()
        is HomeUiState.Success -> VideoList(videos = uiState.videos)
        is HomeUiState.Error -> ErrorView(message = uiState.message)
    }
}
```

### Data Layer (`data/`)

**Responsibilities:**
- Provide data to the application
- Handle data transformations
- Cache data locally
- Fetch data from remote sources

**Components:**
- **Repositories**: Single source of truth for data
- **Data Sources**: Local (Room) and Remote (API)
- **Models**: Data transfer objects

**Example:**
```kotlin
@Singleton
class VideoRepository @Inject constructor(
    private val apiService: ApiService,
    private val database: AppDatabase
) {
    fun getVideos(): Flow<List<Video>> {
        return database.videoDao().getAll()
            .onEach { if (it.isEmpty()) fetchFromApi() }
    }
}
```

### Dependency Injection (`di/`)

**Responsibilities:**
- Provide dependencies throughout the app
- Manage object lifecycles
- Enable testability

**Modules:**
- `AppModule`: Application-level dependencies
- `NetworkModule`: Retrofit, OkHttp
- `DatabaseModule`: Room database
- `RepositoryModule`: Data repositories

## Key Architectural Decisions

### 1. Single Activity Architecture

- One `MainActivity` with Compose navigation
- Screens are composables, not activities
- Benefits: Simpler state management, easier testing

### 2. Unidirectional Data Flow

```
User Action → ViewModel → State Update → UI Recomposition
```

- ViewModels expose immutable state via `StateFlow`
- UI observes state and reacts to changes
- No direct UI manipulation

### 3. Repository Pattern

- Repositories abstract data sources
- Single source of truth for each data type
- Enables offline-first architecture

### 4. Coroutines & Flow

- Asynchronous operations with coroutines
- Reactive streams with Flow
- Structured concurrency in ViewModels

### 5. Dependency Injection with Hilt

- Compile-time DI
- Scoped dependencies (Singleton, Activity, ViewModel)
- Easy testing with fakes/mocks

## Data Flow Example

### Loading Videos

1. **User opens home screen**
   ```
   HomeScreen → HomeViewModel.loadVideos()
   ```

2. **ViewModel requests data**
   ```
   HomeViewModel → VideoRepository.getVideos()
   ```

3. **Repository checks cache**
   ```
   VideoRepository → Room database
   ```

4. **If cache empty, fetch from API**
   ```
   VideoRepository → ApiService.fetchVideos()
   ```

5. **Repository updates cache**
   ```
   VideoRepository → Room database.insert(videos)
   ```

6. **ViewModel updates state**
   ```
   HomeViewModel._uiState.value = HomeUiState.Success(videos)
   ```

7. **UI recomposes with new data**
   ```
   HomeScreen observes uiState → displays videos
   ```

## Testing Strategy

### Unit Tests (`test/`)

- ViewModel logic
- Repository logic
- Utility functions
- Use mocks for dependencies

### Instrumentation Tests (`androidTest/`)

- Compose UI tests
- Integration tests
- Database tests
- Use fake implementations

## Performance Considerations

### Image Loading

- Coil 3.0 for async image loading
- Memory and disk caching
- Automatic request coalescing

### Database

- Room with Flow for reactive queries
- Indexes on frequently queried columns
- Lazy loading of relationships

### Video Playback

- ExoPlayer (Media3) for efficient playback
- Adaptive streaming
- Background playback support

## Security

### API Keys

- Loaded from environment variables
- Never committed to source control
- Build-time injection via BuildConfig

### Signing

- Keystore credentials in `keystore.properties`
- File excluded from version control
- Example provided for setup

## Future Improvements

- [ ] Add offline-first sync
- [ ] Implement caching layer for API responses
- [ ] Add analytics tracking
- [ ] Implement crash reporting
- [ ] Add feature flags system
- [ ] Migrate to Kotlin Multiplatform
