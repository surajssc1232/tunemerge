# TuneMerge

TuneMerge is a Spring Boot web application that allows users to import playlists from their Spotify account and export them to other music streaming platforms.

## Features

- Import playlists from Spotify
- Export playlists to other music streaming platforms (e.g., YouTube Music, Wynk)
- User-friendly web interface
- Secure authentication with Spotify

## Prerequisites

- Java 11 or higher
- Maven
- Spotify Developer Account

## Setup

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/tunemerge.git
   cd tunemerge
   ```

2. Configure Spotify API credentials:
   - Create a Spotify Developer account and register your application
   - Update `src/main/resources/application.properties` with your Spotify client ID and client secret

3. Build the project:
   ```
   mvn clean install
   ```

4. Run the application:
   ```
   mvn spring-boot:run
   ```

5. Open a web browser and navigate to `http://localhost:8080`

## Usage

1. Log in with your Spotify account
2. Select the playlist you want to transfer
3. Choose the target music streaming platform
4. Click "Merge" to export your playlist

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
