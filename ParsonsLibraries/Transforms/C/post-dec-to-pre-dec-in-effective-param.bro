Author=Alessio Gaspar
Title= Convert post-decrement to pre-decrement in effective parameter 
Description=Modify a function call with formal parameter by replacing existing post-decrement to pre-decrement
FragmentSelection=^(.*\W+\w+\s*\(\s*)(.*,\s*)*(\w+\s*)--(\W*.*\).*;\s*)$
Transform=$1$2--$3$4

