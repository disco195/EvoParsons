Author = EvoParsons Team

Title= Functions/Methods (FN)

Description= Removing the first parameter from a function/method prototype

FragmentSelection=^(?!.*(for|if|else\s*if|while|=|System))(.*)\(\s*(double|float|int|char|boolean|short|long|String)\s*\W*\w+\s*\s*,*\s*(.*)$

Transform=$1$2\($4
