-s, --source=<./res>                  Specify where to search for the arb files.
                                      (defaults to "./res")

-o, --output=<./lib/src/generated>    Specify where to save the generated dart files.
                                      (defaults to "./lib/src/generated")

-w, --[no-]watch                      Whether you want to listen for file changes.
                                      NOTE: Keep in mind that the changes are detected when the file is
                                      saved after modification. So if you use an IDE make sure to save
                                      the arb file so that the dart files are updated.

-c, --[no-]create-paths               This will create the folders structure recursevly.
                                      (defaults to on)