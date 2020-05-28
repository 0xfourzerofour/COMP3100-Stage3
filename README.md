# COMP3100-Stage2

This Repository is our implementation of 4 different scheduling algorithms for jobs that have varying resource needs to be run on servers with various different resource capacities

### Prerequisites

```
Linux Environment
```

## Getting Started

open one command line shell and navigate to the `ds-sim` directory and run the server

`./ds-server -c <config_file> -n -v all` 

open up another command line shell tab in the same directory to get ready to run the algorithms

## Running the algoritms

### Cheapest Fit
```
java Client -a cf
```

### allToLargest
```
java Client
```

### Best Fit
```
java Client -a bf
```

### Worst Fit
```
java Client -a wf
```

### First Fit
```
java Client -a ff
```


