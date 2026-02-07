import unittest

from rename_cleanup import RenameOptions, build_rename_plan


class TestRenameCleanup(unittest.TestCase):
    def test_collision_suffix_added(self):
        files = ["My File.txt", "My  File.txt", "my-file-2.txt"]
        options = RenameOptions(
            regex_pattern=None,
            regex_replacement="",
            lowercase=True,
            spaces_to_hyphens=True,
            keep_extension=True,
        )
        plan = build_rename_plan(files, options)
        self.assertEqual(plan["My File.txt"], "my-file.txt")
        self.assertEqual(plan["My  File.txt"], "my-file-3.txt")


if __name__ == "__main__":
    unittest.main()
