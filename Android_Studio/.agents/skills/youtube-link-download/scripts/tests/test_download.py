import unittest

from download import DownloadOptions, build_command, infer_max_downloads


class TestDownloadCommand(unittest.TestCase):
    def test_audio_mode_adds_audio_flags(self):
        options = DownloadOptions(
            mode="audio",
            quality="best",
            output_dir="downloads",
            template="%(title)s.%(ext)s",
            yes_playlist=True,
            max_downloads=None,
            sub_lang="en",
            audio_format="mp3",
        )
        cmd = build_command(options, ["https://www.youtube.com/watch?v=abc123"])
        self.assertIn("-x", cmd)
        self.assertIn("--audio-format", cmd)
        self.assertIn("mp3", cmd)

    def test_subs_mode_skips_download(self):
        options = DownloadOptions(
            mode="subs",
            quality="best",
            output_dir="downloads",
            template="%(title)s.%(ext)s",
            yes_playlist=True,
            max_downloads=None,
            sub_lang="en",
            audio_format="mp3",
        )
        cmd = build_command(options, ["https://www.youtube.com/watch?v=abc123"])
        self.assertIn("--write-subs", cmd)
        self.assertIn("--write-auto-subs", cmd)
        self.assertIn("--skip-download", cmd)

    def test_infer_max_downloads_defaults_for_playlists(self):
        urls = ["https://www.youtube.com/watch?v=zetItcyIKJw&list=RDzetItcyIKJw"]
        self.assertEqual(infer_max_downloads(urls, None), 5)
        self.assertIsNone(infer_max_downloads(["https://www.youtube.com/watch?v=abc"], None))


if __name__ == "__main__":
    unittest.main()
